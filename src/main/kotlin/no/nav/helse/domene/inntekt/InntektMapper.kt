package no.nav.helse.domene.inntekt

import no.nav.helse.domene.inntekt.domain.Inntekt
import no.nav.helse.domene.inntekt.domain.Virksomhet
import no.nav.helse.domene.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*
import org.slf4j.LoggerFactory
import java.time.YearMonth

object InntektMapper {
    private val log = LoggerFactory.getLogger("InntektMapper")

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

    fun mapToIntekt(inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt) =
            mapToVirksomhet(inntekt.virksomhet)?.let { virksomhet ->
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
}
