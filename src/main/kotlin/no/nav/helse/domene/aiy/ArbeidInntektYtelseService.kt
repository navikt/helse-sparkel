package no.nav.helse.domene.aiy

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import no.nav.helse.Feilårsak
import no.nav.helse.arrow.sequenceU
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.domene.arbeid.ArbeidsforholdService
import no.nav.helse.domene.arbeid.domain.Arbeidsforhold
import no.nav.helse.domene.inntekt.InntektService
import no.nav.helse.domene.inntekt.domain.Inntekt
import no.nav.helse.domene.inntekt.domain.Virksomhet
import no.nav.helse.domene.organisasjon.OrganisasjonService
import no.nav.helse.domene.organisasjon.domain.Organisasjon
import no.nav.helse.probe.DatakvalitetProbe
import java.time.LocalDate
import java.time.YearMonth

class ArbeidInntektYtelseService(private val arbeidsforholdService: ArbeidsforholdService,
                                 private val inntektService: InntektService,
                                 private val organisasjonService: OrganisasjonService,
                                 private val datakvalitetProbe: DatakvalitetProbe) {

    companion object {
        private val inntektfilter = "ForeldrepengerA-Inntekt"
    }

    fun finnArbeidInntekterOgYtelser(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            finnInntekterOgFordelEtterType(aktørId, YearMonth.from(fom), YearMonth.from(tom)) { lønnsinntekter, ytelser, pensjonEllerTrygd, næringsinntekter ->
                arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom).flatMap { kombinertArbeidsforholdliste ->
                    finnMuligeArbeidsforholdForInntekter(lønnsinntekter, kombinertArbeidsforholdliste).map { inntekterMedMuligeArbeidsforhold ->
                        ArbeidInntektYtelse(inntekterMedMuligeArbeidsforhold, kombinertArbeidsforholdliste, ytelser, pensjonEllerTrygd, næringsinntekter)
                    }
                }
            }

    private fun <R> finnInntekterOgFordelEtterType(aktørId: AktørId, fom: YearMonth, tom: YearMonth, callback: ArbeidInntektYtelseService.(List<Inntekt.Lønn>, List<Inntekt.Ytelse>, List<Inntekt.PensjonEllerTrygd>, List<Inntekt.Næring>) -> Either<Feilårsak, R>) =
            inntektService.hentInntekter(aktørId, fom, tom, inntektfilter).flatMap { inntekter ->
                val lønnsinntekter = mutableListOf<Inntekt.Lønn>()
                val ytelser = mutableListOf<Inntekt.Ytelse>()
                val pensjonEllerTrygd = mutableListOf<Inntekt.PensjonEllerTrygd>()
                val næringsinntekter = mutableListOf<Inntekt.Næring>()

                inntekter.forEach { inntekt ->
                    when (inntekt) {
                        is Inntekt.Lønn -> lønnsinntekter.add(inntekt)
                        is Inntekt.Ytelse -> ytelser.add(inntekt)
                        is Inntekt.PensjonEllerTrygd -> pensjonEllerTrygd.add(inntekt)
                        is Inntekt.Næring -> næringsinntekter.add(inntekt)
                    }
                }

                this.callback(lønnsinntekter, ytelser, pensjonEllerTrygd, næringsinntekter)
            }

    private fun finnMuligeArbeidsforholdForInntekter(lønnsinntekter: List<Inntekt.Lønn>, arbeidsforholdliste: List<Arbeidsforhold>) =
            lønnsinntekter.map { inntekt ->
                finnMuligeArbeidsforholdForInntekt(inntekt, arbeidsforholdliste).map { muligeArbeidsforhold ->
                    inntekt to muligeArbeidsforhold
                }
            }.sequenceU().also { either ->
                either.map { inntekterMedMuligeArbeidsforhold ->
                    inntekterMedMuligeArbeidsforhold.onEach { (_, arbeidsforhold) ->
                        datakvalitetProbe.tellArbeidsforholdPerInntekt(arbeidsforhold)
                    }.filter { (_, arbeidsforhold) ->
                        arbeidsforhold.isEmpty()
                    }.map { (inntekt, _) ->
                        inntekt
                    }.let { inntekterUtenArbeidsforhold ->
                        datakvalitetProbe.tellAvvikPåInntekter(inntekterUtenArbeidsforhold)
                    }

                    arbeidsforholdliste.filterNot { arbeidsforhold ->
                        inntekterMedMuligeArbeidsforhold.any { (_, muligeArbeidsforhold) ->
                            muligeArbeidsforhold.any { it == arbeidsforhold }
                        }
                    }.let { arbeidsforholdUtenInntekter ->
                        datakvalitetProbe.tellAvvikPåArbeidsforhold(arbeidsforholdUtenInntekter)
                    }
                }
            }

    private fun finnMuligeArbeidsforholdForInntekt(inntekt: Inntekt.Lønn, arbeidsforholdliste: List<Arbeidsforhold>) =
            when (inntekt.virksomhet) {
                is Virksomhet.Person -> arbeidsforholdliste.filter {
                    it.arbeidsgiver is Virksomhet.Person
                }.filter {
                    (it.arbeidsgiver as Virksomhet.Person).personnummer == inntekt.virksomhet.personnummer
                }.right()
                is Virksomhet.Organisasjon -> organisasjonService.hentOrganisasjon(inntekt.virksomhet.organisasjonsnummer).map { organisasjon ->
                    when (organisasjon) {
                        is Organisasjon.Virksomhet -> arbeidsforholdliste.filter {
                            it.arbeidsgiver is Virksomhet.Organisasjon
                        }.filter {
                            it.arbeidsgiver == inntekt.virksomhet || organisasjon.inngårIJuridiskEnhet.any { inngårIJuridiskEnhet ->
                                when (it.arbeidsgiver) {
                                    is Virksomhet.Organisasjon -> inngårIJuridiskEnhet.organisasjonsnummer == (it.arbeidsgiver as Virksomhet.Organisasjon).organisasjonsnummer
                                    else -> false
                                }
                            }
                        }
                        is Organisasjon.JuridiskEnhet -> {
                            arbeidsforholdliste.filter { arbeidsforhold ->
                                arbeidsforhold.arbeidsgiver == inntekt.virksomhet || organisasjon.virksomheter.any { driverVirksomhet ->
                                    driverVirksomhet.virksomhet.orgnr == (arbeidsforhold.arbeidsgiver as Virksomhet.Organisasjon).organisasjonsnummer
                                }
                            }
                        }
                        is Organisasjon.Organisasjonsledd -> {
                            emptyList()
                        }
                    }
                }
                else -> throw NotImplementedError("har ikke implementert støtte for virksomhetstype ${inntekt.virksomhet.type()}")
        }
}
