package no.nav.helse.domene.aiy

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import no.nav.helse.Feilårsak
import no.nav.helse.arrow.sequenceU
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.aiy.domain.Arbeidsforhold
import no.nav.helse.domene.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.domene.aiy.organisasjon.OrganisasjonService
import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon
import no.nav.helse.domene.aiy.inntektskomponenten.UtbetalingOgTrekkService
import no.nav.helse.domene.aiy.domain.UtbetalingEllerTrekk
import no.nav.helse.domene.aiy.domain.Virksomhet
import no.nav.helse.probe.DatakvalitetProbe
import java.time.LocalDate
import java.time.YearMonth

class ArbeidInntektYtelseService(private val arbeidsforholdService: ArbeidsforholdService,
                                 private val utbetalingOgTrekkService: UtbetalingOgTrekkService,
                                 private val organisasjonService: OrganisasjonService,
                                 private val datakvalitetProbe: DatakvalitetProbe) {

    companion object {
        private val inntektfilter = "ForeldrepengerA-Inntekt"
    }

    fun finnArbeidInntekterOgYtelser(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            hentUtbetalingerEllerTrekkOgFordelEtterType(aktørId, YearMonth.from(fom), YearMonth.from(tom)) { lønnsinntekter, ytelser, pensjonEllerTrygd, næringsinntekter ->
                arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom).flatMap { kombinertArbeidsforholdliste ->
                    finnMuligeArbeidsforholdForLønnsutbetalinger(lønnsinntekter, kombinertArbeidsforholdliste).map { inntekterMedMuligeArbeidsforhold ->
                        ArbeidInntektYtelse(inntekterMedMuligeArbeidsforhold, kombinertArbeidsforholdliste, ytelser, pensjonEllerTrygd, næringsinntekter).also {
                            datakvalitetProbe.inspiserArbeidInntektYtelse(it)
                        }
                    }
                }
            }

    private fun <R> hentUtbetalingerEllerTrekkOgFordelEtterType(aktørId: AktørId, fom: YearMonth, tom: YearMonth, callback: ArbeidInntektYtelseService.(List<UtbetalingEllerTrekk.Lønn>, List<UtbetalingEllerTrekk.Ytelse>, List<UtbetalingEllerTrekk.PensjonEllerTrygd>, List<UtbetalingEllerTrekk.Næring>) -> Either<Feilårsak, R>) =
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, fom, tom, inntektfilter).flatMap { inntekter ->
                val lønnsinntekter = mutableListOf<UtbetalingEllerTrekk.Lønn>()
                val ytelser = mutableListOf<UtbetalingEllerTrekk.Ytelse>()
                val pensjonEllerTrygd = mutableListOf<UtbetalingEllerTrekk.PensjonEllerTrygd>()
                val næringsinntekter = mutableListOf<UtbetalingEllerTrekk.Næring>()

                inntekter.forEach { inntekt ->
                    when (inntekt) {
                        is UtbetalingEllerTrekk.Lønn -> lønnsinntekter.add(inntekt)
                        is UtbetalingEllerTrekk.Ytelse -> ytelser.add(inntekt)
                        is UtbetalingEllerTrekk.PensjonEllerTrygd -> pensjonEllerTrygd.add(inntekt)
                        is UtbetalingEllerTrekk.Næring -> næringsinntekter.add(inntekt)
                    }
                }

                this.callback(lønnsinntekter, ytelser, pensjonEllerTrygd, næringsinntekter)
            }

    private fun finnMuligeArbeidsforholdForLønnsutbetalinger(lønnsinntekter: List<UtbetalingEllerTrekk.Lønn>, arbeidsforholdliste: List<Arbeidsforhold>) =
            lønnsinntekter.map { inntekt ->
                finnMuligeArbeidsforholdForLønnsutbetaling(inntekt, arbeidsforholdliste).map { muligeArbeidsforhold ->
                    inntekt to muligeArbeidsforhold
                }
            }.sequenceU()

    private fun finnMuligeArbeidsforholdForLønnsutbetaling(utbetalingEllerTrekk: UtbetalingEllerTrekk.Lønn, arbeidsforholdliste: List<Arbeidsforhold>) =
            when (utbetalingEllerTrekk.virksomhet) {
                is Virksomhet.Person -> arbeidsforholdliste.filter {
                    it.arbeidsgiver is Virksomhet.Person
                }.filter {
                    (it.arbeidsgiver as Virksomhet.Person).personnummer == utbetalingEllerTrekk.virksomhet.personnummer
                }.right()
                is Virksomhet.Organisasjon -> organisasjonService.hentOrganisasjon(utbetalingEllerTrekk.virksomhet.organisasjonsnummer).map { organisasjon ->
                    when (organisasjon) {
                        is Organisasjon.Virksomhet -> arbeidsforholdliste.filter {
                            it.arbeidsgiver is Virksomhet.Organisasjon
                        }.filter {
                            it.arbeidsgiver == utbetalingEllerTrekk.virksomhet || organisasjon.inngårIJuridiskEnhet.any { inngårIJuridiskEnhet ->
                                when (it.arbeidsgiver) {
                                    is Virksomhet.Organisasjon -> inngårIJuridiskEnhet.organisasjonsnummer == (it.arbeidsgiver as Virksomhet.Organisasjon).organisasjonsnummer
                                    else -> false
                                }
                            }
                        }
                        is Organisasjon.JuridiskEnhet -> {
                            arbeidsforholdliste.filter { arbeidsforhold ->
                                arbeidsforhold.arbeidsgiver is Virksomhet.Organisasjon
                            }.filter { arbeidsforhold ->
                                arbeidsforhold.arbeidsgiver == utbetalingEllerTrekk.virksomhet || organisasjon.virksomheter.any { driverVirksomhet ->
                                    driverVirksomhet.virksomhet.orgnr == (arbeidsforhold.arbeidsgiver as Virksomhet.Organisasjon).organisasjonsnummer
                                }
                            }
                        }
                        is Organisasjon.Organisasjonsledd -> {
                            emptyList()
                        }
                    }
                }
                else -> throw NotImplementedError("har ikke implementert støtte for virksomhetstype ${utbetalingEllerTrekk.virksomhet.type()}")
        }
}
