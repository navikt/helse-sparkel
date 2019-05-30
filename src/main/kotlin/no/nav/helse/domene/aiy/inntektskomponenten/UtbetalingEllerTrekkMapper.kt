package no.nav.helse.domene.aiy.inntektskomponenten

import no.nav.helse.domene.aiy.domain.UtbetalingEllerTrekk
import no.nav.helse.domene.aiy.domain.Virksomhet
import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*
import org.slf4j.LoggerFactory
import java.time.YearMonth

object UtbetalingEllerTrekkMapper {
    private val log = LoggerFactory.getLogger(UtbetalingEllerTrekkMapper::class.java)

    fun mapToVirksomhet(aktør: Aktoer) =
            when (aktør) {
                is Organisasjon -> Virksomhet.Organisasjon(Organisasjonsnummer(aktør.orgnummer))
                is PersonIdent -> Virksomhet.Person(aktør.personIdent)
                is AktoerId -> Virksomhet.NavAktør(aktør.aktoerId)
                else -> {
                    log.warn("ukjent virksomhet: ${aktør.javaClass.name}")
                    null
                }
            }

    fun mapToUtbetalingEllerTrekk(inntekt: Inntekt) =
            mapToVirksomhet(inntekt.virksomhet)?.let { virksomhet ->
                val utbetalingsperiode = YearMonth.of(inntekt.utbetaltIPeriode.year, inntekt.utbetaltIPeriode.month)

                when (inntekt) {
                    is YtelseFraOffentlige -> {
                        UtbetalingEllerTrekk.Ytelse(
                                virksomhet = virksomhet,
                                utbetalingsperiode = utbetalingsperiode,
                                beløp = inntekt.beloep,
                                kode = inntekt.beskrivelse.value)
                    }
                    is PensjonEllerTrygd -> {
                        UtbetalingEllerTrekk.PensjonEllerTrygd(
                                virksomhet = virksomhet,
                                utbetalingsperiode = utbetalingsperiode,
                                beløp = inntekt.beloep,
                                kode = inntekt.beskrivelse.value)
                    }
                    is Naeringsinntekt -> {
                        UtbetalingEllerTrekk.Næring(
                                virksomhet = virksomhet,
                                utbetalingsperiode = utbetalingsperiode,
                                beløp = inntekt.beloep,
                                kode = inntekt.beskrivelse.value)
                    }
                    is Loennsinntekt -> {
                        UtbetalingEllerTrekk.Lønn(
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
}
