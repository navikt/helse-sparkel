package no.nav.helse.ws.aiy

import io.prometheus.client.Counter
import no.nav.helse.flatMap
import no.nav.helse.map
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdService
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsgiver
import no.nav.helse.ws.inntekt.InntektService
import no.nav.helse.ws.inntekt.domain.Inntekt
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class ArbeidInntektYtelseService(private val arbeidsforholdService: ArbeidsforholdService, private val inntektService: InntektService) {

    companion object {
        private val log = LoggerFactory.getLogger("ArbeidInntektYtelseService")

        private val arbeidsforholdAvviksCounter = Counter.build()
                .name("arbeidsforhold_avvik_totals")
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
                    arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom).map { arbeidsforholdliste ->
                        val arbeidsforhold = grupperteInntekter[Inntekt.Lønn::class.java].orEmpty().mapValues { entry ->
                            entry.value.map { inntekt ->
                                inntekt as Inntekt.Lønn
                            }
                        }.filterKeys {  virksomhet ->
                            arbeidsforholdliste.any { arbeidsforhold ->
                                virksomhet.identifikator == (arbeidsforhold.arbeidsgiver as Arbeidsgiver.Virksomhet).virksomhet.orgnr.value
                            }
                        }.mapKeys { entry ->
                            arbeidsforholdliste.first { arbeidsforhold ->
                                entry.key.identifikator == (arbeidsforhold.arbeidsgiver as Arbeidsgiver.Virksomhet).virksomhet.orgnr.value
                            }
                        }.also { inntekter ->
                            arbeidsforholdliste.forEach { arbeidsforhold ->
                                if (!inntekter.containsKey(arbeidsforhold)) {
                                    arbeidsforholdAvviksCounter.inc()
                                    log.warn("did not find inntekter for arbeidsforhold with arbeidsgiver=${arbeidsforhold.arbeidsgiver}")
                                }
                            }

                            grupperteInntekter[Inntekt.Lønn::class.java]?.forEach { entry ->
                                if (inntekter.keys.firstOrNull { arbeidsforhold ->
                                    entry.key.identifikator == (arbeidsforhold.arbeidsgiver as Arbeidsgiver.Virksomhet).virksomhet.orgnr.value
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
