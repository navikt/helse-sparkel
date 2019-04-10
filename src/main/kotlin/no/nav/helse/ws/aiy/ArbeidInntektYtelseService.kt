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
import no.nav.helse.ws.inntekt.domain.Inntekt
import no.nav.helse.ws.inntekt.domain.Virksomhet
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class ArbeidInntektYtelseService(private val arbeidsforholdService: ArbeidsforholdService, private val inntektService: InntektService) {

    companion object {
        private val log = LoggerFactory.getLogger("ArbeidInntektYtelseService")

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

    fun finnArbeidsforholdMedInntekter(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            inntektService.hentInntekter(aktørId, YearMonth.from(fom), YearMonth.from(tom)).flatMap { inntekter ->
                inntekter.groupBy { inntekt ->
                    inntekt::class.java
                }.mapValues { entry ->
                    entry.value.groupBy {  inntekt ->
                        inntekt.virksomhet
                    }
                }.let { grupperteInntekter ->
                    inntektService.hentFrilansarbeidsforhold(aktørId, YearMonth.from(fom), YearMonth.from(tom)).flatMap { frilansArbeidsforholdliste ->
                        arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom).map { arbeidsforholdliste ->
                            arbeidsforholdliste.map { arbeidsforhold ->
                                Arbeidsforhold.Arbeidstaker(
                                        arbeidsgiver = when (arbeidsforhold.arbeidsgiver) {
                                            is Arbeidsgiver.Virksomhet -> Virksomhet.Organisasjon(arbeidsforhold.arbeidsgiver.virksomhet.orgnr)
                                            is Arbeidsgiver.Person -> Virksomhet.Person(arbeidsforhold.arbeidsgiver.personnummer)
                                        },
                                        startdato = arbeidsforhold.startdato,
                                        sluttdato = arbeidsforhold.sluttdato
                                )
                            }.plus(frilansArbeidsforholdliste.map { arbeidsforhold ->
                                Arbeidsforhold.Frilans(
                                        arbeidsgiver = arbeidsforhold.arbeidsgiver,
                                        startdato = arbeidsforhold.startdato,
                                        sluttdato = arbeidsforhold.sluttdato,
                                        yrke = arbeidsforhold.yrke
                                )
                            }).let { kombinertArbeidsforholdliste ->
                                val arbeidsforhold = grupperteInntekter[Inntekt.Lønn::class.java].orEmpty().mapValues { entry ->
                                    entry.value.map { inntekt ->
                                        inntekt as Inntekt.Lønn
                                    }
                                }.filterKeys { virksomhet ->
                                    kombinertArbeidsforholdliste.any { arbeidsforhold ->
                                        virksomhet.identifikator == arbeidsforhold.arbeidsgiver.identifikator
                                    }
                                }.mapKeys { entry ->
                                    kombinertArbeidsforholdliste.first { arbeidsforhold ->
                                        entry.key.identifikator == arbeidsforhold.arbeidsgiver.identifikator
                                    }
                                }.mapValues {
                                    it.value.groupBy {
                                        it.utbetalingsperiode
                                    }
                                }.also { inntekter ->
                                    kombinertArbeidsforholdliste.forEach { arbeidsforhold ->
                                        if (!inntekter.containsKey(arbeidsforhold)) {
                                            arbeidsforholdAvviksCounter.labels(arbeidsforhold.type()).inc()
                                            log.warn("did not find inntekter for arbeidsforhold (${arbeidsforhold.type()}) with arbeidsgiver=${arbeidsforhold.arbeidsgiver}")
                                        }
                                    }

                                    grupperteInntekter[Inntekt.Lønn::class.java]?.forEach { entry ->
                                        if (inntekter.keys.firstOrNull { arbeidsforhold ->
                                                    entry.key.identifikator == arbeidsforhold.arbeidsgiver.identifikator
                                                } == null) {
                                            inntektAvviksCounter.inc(entry.value.size.toDouble())
                                            log.warn("did not find arbeidsforhold for ${entry.value.size} inntekter (${entry.value.joinToString { it.type() }}) with arbeidsgiver=${entry.key}")
                                        }
                                    }
                                }

                                val ytelser = grupperteInntekter[Inntekt.Ytelse::class.java].orEmpty().mapValues { inntektEtterVirksomhet ->
                                    inntektEtterVirksomhet.value.map { inntekt ->
                                        inntekt as Inntekt.Ytelse
                                    }
                                }
                                val pensjonEllerTrygd = grupperteInntekter[Inntekt.PensjonEllerTrygd::class.java].orEmpty().mapValues { inntektEtterVirksomhet ->
                                    inntektEtterVirksomhet.value.map { inntekt ->
                                        inntekt as Inntekt.PensjonEllerTrygd
                                    }
                                }
                                val næringsinntekt = grupperteInntekter[Inntekt.Næring::class.java].orEmpty().mapValues { inntektEtterVirksomhet ->
                                    inntektEtterVirksomhet.value.map { inntekt ->
                                        inntekt as Inntekt.Næring
                                    }
                                }

                                ArbeidInntektYtelse(arbeidsforhold, ytelser, pensjonEllerTrygd, næringsinntekt)
                            }
                        }
                    }
                }
            }
}
