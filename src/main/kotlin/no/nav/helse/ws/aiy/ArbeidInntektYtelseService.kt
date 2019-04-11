package no.nav.helse.ws.aiy

import io.prometheus.client.Counter
import no.nav.helse.flatMap
import no.nav.helse.map
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.ws.aiy.domain.Arbeidsforhold
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdService
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsgiver
import no.nav.helse.ws.inntekt.InntektService
import no.nav.helse.ws.inntekt.domain.ArbeidsforholdFrilanser
import no.nav.helse.ws.inntekt.domain.Inntekt
import no.nav.helse.ws.inntekt.domain.Virksomhet
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class ArbeidInntektYtelseService(private val arbeidsforholdService: ArbeidsforholdService, private val inntektService: InntektService) {

    companion object {
        private val log = LoggerFactory.getLogger(ArbeidInntektYtelseService::class.java)

        private val arbeidsforholdAvviksCounter = Counter.build()
                .name("arbeidsforhold_avvik_totals")
                .labelNames("type")
                .help("antall arbeidsforhold som ikke har noen tilhørende inntekter")
                .register()
        private val inntektAvviksCounter = Counter.build()
                .name("inntekt_avvik_totals")
                .help("antall inntekter som ikke har noen tilhørende arbeidsforhold")
                .register()
    }

    fun finnArbeidInntekterOgYtelser(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            finnInntekterGruppertPåTypeOgVirksomhet(aktørId, YearMonth.from(fom), YearMonth.from(tom)).flatMap { inntekter ->
                finnOgKombinerArbeidsforholdOgFrilansArbeidsforhold(aktørId, fom, tom).map { kombinertArbeidsforholdliste ->
                    val arbeidsforhold = kombinerArbeidsforholdOgInntekt(inntekter, kombinertArbeidsforholdliste)

                    val ytelser = grupperInntektPåType<Inntekt.Ytelse>(inntekter)
                    val pensjonEllerTrygd = grupperInntektPåType<Inntekt.PensjonEllerTrygd>(inntekter)
                    val næringsinntekt = grupperInntektPåType<Inntekt.Næring>(inntekter)

                    ArbeidInntektYtelse(arbeidsforhold, ytelser, pensjonEllerTrygd, næringsinntekt)
                }
            }

    private fun finnInntekterGruppertPåTypeOgVirksomhet(aktørId: AktørId, fom: YearMonth, tom: YearMonth) =
            inntektService.hentInntekter(aktørId, fom, tom).map { inntekter ->
                inntekter.groupBy { inntekt ->
                    inntekt::class.java
                }.mapValues { entry ->
                    entry.value.groupBy {  inntekt ->
                        inntekt.virksomhet
                    }
                }
            }

    private fun finnOgKombinerArbeidsforholdOgFrilansArbeidsforhold(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            inntektService.hentFrilansarbeidsforhold(aktørId, YearMonth.from(fom), YearMonth.from(tom)).flatMap { frilansArbeidsforholdliste ->
                arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom).map { arbeidsforholdliste ->
                    kombinerArbeidsforhold(arbeidsforholdliste, frilansArbeidsforholdliste)
                }
            }

    private fun kombinerArbeidsforhold(arbeidstakerArbeidsforhold: List<no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold>, frilansArbeidsforhold: List<ArbeidsforholdFrilanser>) =
            arbeidstakerArbeidsforhold.map(::tilArbeidsforhold)
                    .plus(frilansArbeidsforhold.map(::tilArbeidsforhold))

    private fun tilArbeidsforhold(arbeidsforhold: no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold) =
            Arbeidsforhold.Arbeidstaker(
                    arbeidsgiver = when (arbeidsforhold.arbeidsgiver) {
                        is Arbeidsgiver.Virksomhet -> Virksomhet.Organisasjon(arbeidsforhold.arbeidsgiver.virksomhet.orgnr)
                        is Arbeidsgiver.Person -> Virksomhet.Person(arbeidsforhold.arbeidsgiver.personnummer)
                    },
                    startdato = arbeidsforhold.startdato,
                    sluttdato = arbeidsforhold.sluttdato
            )
    private fun tilArbeidsforhold(arbeidsforhold: ArbeidsforholdFrilanser) =
            Arbeidsforhold.Frilans(
                    arbeidsgiver = arbeidsforhold.arbeidsgiver,
                    startdato = arbeidsforhold.startdato,
                    sluttdato = arbeidsforhold.sluttdato,
                    yrke = arbeidsforhold.yrke
            )


    private fun kombinerArbeidsforholdOgInntekt(inntekter: Map<Class<out Inntekt>, Map<Virksomhet, List<Inntekt>>>, arbeidsforholdliste: List<Arbeidsforhold>) =
            grupperInntektPåType<Inntekt.Lønn>(inntekter).let { lønnsinntekter ->
                lønnsinntekter.filterKeys { virksomhet ->
                    arbeidsforholdliste.any { arbeidsforhold ->
                        virksomhet.identifikator == arbeidsforhold.arbeidsgiver.identifikator
                    }
                }.mapKeys { entry ->
                    arbeidsforholdliste.first { arbeidsforhold ->
                        entry.key.identifikator == arbeidsforhold.arbeidsgiver.identifikator
                    }
                }.mapValues { entry ->
                    entry.value.groupBy { lønnsinntekt ->
                        lønnsinntekt.utbetalingsperiode
                    }
                }.also { inntekter ->
                    tellAvvikPåArbeidsforhold(arbeidsforholdliste, inntekter)
                }.also { inntekter ->
                    tellAvvikPåInntekter(lønnsinntekter, inntekter)
                }
            }

    private fun tellAvvikPåArbeidsforhold(arbeidsforholdliste: List<Arbeidsforhold>, inntekterFordeltPåArbeidsforhold: Map<Arbeidsforhold, Map<YearMonth, List<Inntekt.Lønn>>>) =
            arbeidsforholdliste.forEach { arbeidsforhold ->
                if (!inntekterFordeltPåArbeidsforhold.containsKey(arbeidsforhold)) {
                    arbeidsforholdAvviksCounter.labels(arbeidsforhold.type()).inc()
                    log.warn("did not find inntekter for arbeidsforhold (${arbeidsforhold.type()}) with arbeidsgiver=${arbeidsforhold.arbeidsgiver}")
                }
            }

    private fun tellAvvikPåInntekter(lønnsinntekter: Map<Virksomhet, List<Inntekt.Lønn>>, inntekterFordeltPåArbeidsforhold: Map<Arbeidsforhold, Map<YearMonth, List<Inntekt.Lønn>>>) =
            lønnsinntekter.forEach { entry ->
                if (inntekterFordeltPåArbeidsforhold.keys.firstOrNull { arbeidsforhold ->
                            entry.key.identifikator == arbeidsforhold.arbeidsgiver.identifikator
                        } == null) {
                    inntektAvviksCounter.inc(entry.value.size.toDouble())
                    log.warn("did not find arbeidsforhold for ${entry.value.size} inntekter (${entry.value.joinToString { it.type() }}) with arbeidsgiver=${entry.key}")
                }
            }

    private inline fun <reified T: Inntekt> grupperInntektPåType(inntekter: Map<Class<out Inntekt>, Map<Virksomhet, List<Inntekt>>>) =
            inntekter[T::class.java].orEmpty().mapValues { inntektEtterVirksomhet ->
                inntektEtterVirksomhet.value.map { inntekt ->
                    inntekt as T
                }
            }
}
