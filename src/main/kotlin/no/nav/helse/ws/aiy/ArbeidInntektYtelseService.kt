package no.nav.helse.ws.aiy

import io.prometheus.client.Counter
import no.nav.helse.flatMap
import no.nav.helse.map
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.aiy.domain.ArbeidsforholdMedInntekt
import no.nav.helse.ws.aiy.domain.InntektUtenArbeidsgiver
import no.nav.helse.ws.arbeidsforhold.Arbeidsforhold
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdService
import no.nav.helse.ws.arbeidsforhold.Arbeidsgiver
import no.nav.helse.ws.inntekt.InntektService
import no.nav.helse.ws.inntekt.Opptjeningsperiode
import org.slf4j.LoggerFactory
import java.math.BigDecimal
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
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom).flatMap { arbeidsforholdliste ->
                inntektService.hentInntekter(aktørId, YearMonth.from(fom), YearMonth.from(tom)).map { inntekter ->
                    inntekter.groupBy { inntekt ->
                        inntekt.arbeidsgiver
                    }.mapValues { entry ->
                        entry.value.map { inntekt ->
                            InntektUtenArbeidsgiver(YearMonth.from(inntekt.opptjeningsperiode.fom), inntekt.beløp)
                        }
                    }.also { grupperteInntekter ->
                        arbeidsforholdliste.forEach { arbeidsforhold ->
                            grupperteInntekter.keys.filter { arbeidsgiver ->
                                (arbeidsgiver as no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon).orgnr ==
                                        (arbeidsforhold.arbeidsgiver as Arbeidsgiver.Organisasjon).orgnummer
                            }.ifEmpty {
                                arbeidsforholdAvviksCounter.inc()
                                log.warn("did not find inntekter for arbeidsforhold with arbeidsgiver=${arbeidsforhold.arbeidsgiver}")
                            }
                        }
                    }.map { entry ->
                        arbeidsforholdliste.firstOrNull { arbeidsforhold ->
                            (arbeidsforhold.arbeidsgiver as Arbeidsgiver.Organisasjon).orgnummer ==
                                    (entry.key as no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon).orgnr
                        }?.let {
                            ArbeidsforholdMedInntekt(it, entry.value)
                        } ?: run {
                            inntektAvviksCounter.inc(entry.value.size.toDouble())
                            log.warn("did not find arbeidsforhold for ${entry.value.size} inntekter with arbeidsgiver=${entry.key}")
                            null
                        }
                    }.filterNotNull()
                }
            }
}
