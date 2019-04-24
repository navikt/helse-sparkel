package no.nav.helse.domene.inntekt

import io.prometheus.client.Counter
import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.inntekt.domain.Inntekt
import no.nav.helse.domene.inntekt.domain.Virksomhet
import no.nav.helse.domene.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*
import org.slf4j.LoggerFactory
import java.time.YearMonth

object InntektMapper {
    private val log = LoggerFactory.getLogger("InntektMapper")

    private val inntektCounter = Counter.build()
            .name("inntekt_totals")
            .help("antall inntekter mottatt, fordelt på inntektstype")
            .labelNames("type")
            .register()
    private val andreAktørerCounter = Counter.build()
            .name("inntekt_andre_aktorer_totals")
            .help("antall inntekter mottatt med andre aktører enn den det ble gjort oppslag på")
            .register()
    private val inntekterUtenforPeriodeCounter = Counter.build()
            .name("inntekt_utenfor_periode_totals")
            .help("antall inntekter med periode (enten opptjeningsperiode eller utbetaltIPeriode) utenfor søkeperioden")
            .register()
    private val inntektPeriodeCounter = Counter.build()
            .name("inntekt_periode_totals")
            .help("antall inntekter fordelt på periode (opptjeningsperiode eller utbetaltIPeriode")
            .labelNames("type")
            .register()
    private val inntektArbeidsgivertypeCounter = Counter.build()
            .name("inntekt_arbeidsgivertype_totals")
            .labelNames("type")
            .help("antall inntekter fordelt på ulike arbeidsgivertyper")
            .register()
    private val ugyldigOpptjeningsperiodeCounter = Counter.build()
            .name("inntekt_ugyldig_opptjeningsperiode_totals")
            .labelNames("type")
            .help("antall inntekter med ugyldig opptjeningsperiode")
            .register()

    fun mapToInntekt(aktørId: AktørId, fom: YearMonth, tom: YearMonth, arbeidsInntektIdentListe: List<ArbeidsInntektIdent>) =
            arbeidsInntektIdentListe.flatMap {
                it.arbeidsInntektMaaned
            }.flatMap {
                it.arbeidsInntektInformasjon.inntektListe
            }.onEach {
                inntektCounter.labels(when (it) {
                    is YtelseFraOffentlige -> "ytelse"
                    is PensjonEllerTrygd -> "pensjonEllerTrygd"
                    is Naeringsinntekt -> "næring"
                    is Loennsinntekt -> "lønn"
                    else -> "ukjent"
                }).inc()
            }.filter {
                if (erSammeAktør(aktørId)(it)) {
                    true
                } else {
                    andreAktørerCounter.inc()
                    false
                }
            }
                    .onEach {
                        if (it.opptjeningsperiode != null) {
                            inntektPeriodeCounter.labels("opptjeningsperiode").inc()
                        } else {
                            inntektPeriodeCounter.labels("utbetaltIPeriode").inc()
                        }
                    }
                    .onEach {
                        inntektArbeidsgivertypeCounter.labels(when (it.virksomhet) {
                            is Organisasjon -> "organisasjon"
                            is PersonIdent -> "personIdent"
                            is AktoerId -> "aktoerId"
                            else -> "ukjent"
                        }).inc()
                    }
                    .filter {
                        if (erUtbetalingsperiodeInnenforPeriode(fom, tom, it)) {
                            true
                        } else {
                            inntekterUtenforPeriodeCounter.inc()
                            false
                        }
                    }
                    .onEach {
                        if (it.opptjeningsperiode != null) {
                            val opptjeningsperiodeFom = it.opptjeningsperiode.startDato.toLocalDate()
                            val opptjeningsperiodeTom = it.opptjeningsperiode.sluttDato.toLocalDate()

                            if (opptjeningsperiodeFom.withDayOfMonth(1) != opptjeningsperiodeTom.withDayOfMonth(1)) {
                                ugyldigOpptjeningsperiodeCounter.labels("periode_over_en_kalendermåned").inc()
                            }
                            if (opptjeningsperiodeTom < opptjeningsperiodeFom) {
                                ugyldigOpptjeningsperiodeCounter.labels("fom_er_større_enn_tom").inc()
                            }
                        }
                    }
                    .map { inntekt ->
                        when (inntekt.virksomhet) {
                            is Organisasjon -> Virksomhet.Organisasjon(Organisasjonsnummer((inntekt.virksomhet as Organisasjon).orgnummer))
                            is PersonIdent -> Virksomhet.Person((inntekt.virksomhet as PersonIdent).personIdent)
                            is AktoerId -> Virksomhet.NavAktør((inntekt.virksomhet as AktoerId).aktoerId)
                            else -> {
                                log.warn("ukjent virksomhet: ${inntekt.virksomhet.javaClass.name}")
                                null
                            }
                        }?.let { virksomhet ->
                            val utbetalingsperiode = YearMonth.of(inntekt.utbetaltIPeriode.year, inntekt.utbetaltIPeriode.month)

                            when (inntekt) {
                                is YtelseFraOffentlige -> {
                                    Inntekt.Ytelse(
                                            virksomhet = virksomhet,
                                            utbetalingsperiode = utbetalingsperiode,
                                            beløp = inntekt.beloep,
                                            kode = inntekt.beskrivelse.value)
                                }
                                is PensjonEllerTrygd -> {
                                    Inntekt.PensjonEllerTrygd(
                                            virksomhet = virksomhet,
                                            utbetalingsperiode = utbetalingsperiode,
                                            beløp = inntekt.beloep,
                                            kode = inntekt.beskrivelse.value)
                                }
                                is Naeringsinntekt -> {
                                    Inntekt.Næring(
                                            virksomhet = virksomhet,
                                            utbetalingsperiode = utbetalingsperiode,
                                            beløp = inntekt.beloep,
                                            kode = inntekt.beskrivelse.value)
                                }
                                is Loennsinntekt -> {
                                    Inntekt.Lønn(
                                            virksomhet = virksomhet,
                                            utbetalingsperiode = utbetalingsperiode,
                                            beløp = inntekt.beloep)
                                }
                                else -> {
                                    log.error("ukjent inntektstype ${inntekt.javaClass.name}")
                                    null
                                }
                            }
                        }
                    }.filterNotNull()

    private fun erSammeAktør(aktørId: AktørId) = { inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt ->
        if (inntekt.inntektsmottaker is AktoerId) {
            if ((inntekt.inntektsmottaker as AktoerId).aktoerId == aktørId.aktor) {
                true
            } else {
                log.warn("Inntekt gjelder for annen aktør (${(inntekt.inntektsmottaker as AktoerId).aktoerId}) enn det vi forventet (${aktørId.aktor})")
                false
            }
        } else {
            log.warn("inntektsmottaker er ikke en AktørId")
            false
        }
    }

    private fun erUtbetalingsperiodeInnenforPeriode(fom: YearMonth, tom: YearMonth, inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt) =
            if (fom.atDay(1) <= inntekt.utbetaltIPeriode.toLocalDate()
                    && tom.atEndOfMonth() >= inntekt.utbetaltIPeriode.toLocalDate()) {
                true
            } else {
                log.info("utbetaltIPeriode (${inntekt.utbetaltIPeriode.toLocalDate()}) er utenfor perioden")
                false
            }
}
