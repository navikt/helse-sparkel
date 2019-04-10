package no.nav.helse.ws.inntekt

import no.nav.helse.ws.inntekt.domain.Inntekt
import no.nav.helse.ws.inntekt.domain.Opptjeningsperiode
import no.nav.helse.ws.inntekt.domain.Virksomhet
import no.nav.helse.ws.inntekt.dto.ArbeidsgiverDTO
import no.nav.helse.ws.inntekt.dto.InntektDTO
import java.time.YearMonth

object InntektDtoMapper {

    fun toDto(virksomhet: Virksomhet) = ArbeidsgiverDTO(virksomhet.identifikator, virksomhet.type())

    fun toDto(inntekt: Inntekt) =
            InntektDTO(
                arbeidsgiver = toDto(inntekt.virksomhet),
                utbetalingsperiode = inntekt.utbetalingsperiode,
                opptjeningsperiode = opptjeningsperiode(inntekt.utbetalingsperiode),
                beløp = inntekt.beløp,
                ytelse = inntekt.isYtelse(),
                kode = inntekt.kode())

    private fun opptjeningsperiode(utbetalingsperiode: YearMonth) =
            Opptjeningsperiode(utbetalingsperiode.atDay(1), utbetalingsperiode.atEndOfMonth())
}
