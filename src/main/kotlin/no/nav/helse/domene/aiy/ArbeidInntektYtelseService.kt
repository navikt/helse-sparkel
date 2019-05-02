package no.nav.helse.domene.aiy

import arrow.core.Either
import arrow.core.flatMap
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
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
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class ArbeidInntektYtelseService(private val arbeidsforholdService: ArbeidsforholdService, private val inntektService: InntektService, private val organisasjonService: OrganisasjonService) {

    companion object {
        private val log = LoggerFactory.getLogger(ArbeidInntektYtelseService::class.java)

        private val inntektfilter = "ForeldrepengerA-Inntekt"

        private val arbeidsforholdAvviksCounter = Counter.build()
                .name("arbeidsforhold_avvik_totals")
                .labelNames("type")
                .help("antall arbeidsforhold som ikke har noen tilhørende inntekter")
                .register()

        private val inntektAvviksCounter = Counter.build()
                .name("inntekt_avvik_totals")
                .help("antall inntekter som ikke har noen tilhørende arbeidsforhold")
                .register()

        private val arbeidsforholdPerInntektHistogram = Histogram.build()
                .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0)
                .name("arbeidsforhold_per_inntekt_sizes")
                .help("fordeling over hvor mange potensielle arbeidsforhold en inntekt har")
                .register()
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
                        arbeidsforholdPerInntektHistogram.observe(arbeidsforhold.size.toDouble())
                    }.filter { (_, arbeidsforhold) ->
                        arbeidsforhold.isEmpty()
                    }.map { (inntekt, _) ->
                        inntekt
                    }.let { inntekterUtenArbeidsforhold ->
                        tellAvvikPåInntekter(inntekterUtenArbeidsforhold)
                    }

                    arbeidsforholdliste.filterNot { arbeidsforhold ->
                        inntekterMedMuligeArbeidsforhold.any { (_, muligeArbeidsforhold) ->
                            muligeArbeidsforhold.any { it == arbeidsforhold }
                        }
                    }.let { arbeidsforholdUtenInntekter ->
                        tellAvvikPåArbeidsforhold(arbeidsforholdUtenInntekter)
                    }
                }
            }

    private fun tellAvvikPåArbeidsforhold(arbeidsforholdliste: List<Arbeidsforhold>) {
        if (arbeidsforholdliste.isNotEmpty()) {
            arbeidsforholdliste.forEach { arbeidsforhold ->
                arbeidsforholdAvviksCounter.labels(arbeidsforhold.type()).inc()
                log.info("did not find inntekter for arbeidsforhold (${arbeidsforhold.type()}) with arbeidsgiver=${arbeidsforhold.arbeidsgiver}")
            }
        }
    }

    private fun tellAvvikPåInntekter(inntekter: List<Inntekt.Lønn>) {
        if (inntekter.isNotEmpty()) {
            inntektAvviksCounter.inc(inntekter.size.toDouble())
            log.info("did not find arbeidsforhold for ${inntekter.size} inntekter: ${inntekter.joinToString { "${it.type()} - ${it.virksomhet}" }}")
        }
    }

    private fun finnMuligeArbeidsforholdForInntekt(inntekt: Inntekt.Lønn, arbeidsforholdliste: List<Arbeidsforhold>) =
            organisasjonService.hentOrganisasjon((inntekt.virksomhet as Virksomhet.Organisasjon).organisasjonsnummer).map { organisasjon ->
                when (organisasjon) {
                    is Organisasjon.Virksomhet -> arbeidsforholdliste.filter {
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
}
