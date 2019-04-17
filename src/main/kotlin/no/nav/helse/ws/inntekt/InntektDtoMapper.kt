package no.nav.helse.ws.inntekt

import no.nav.helse.ws.inntekt.domain.Inntekt
import no.nav.helse.ws.inntekt.domain.Virksomhet
import no.nav.helse.ws.inntekt.dto.InntektDTO
import no.nav.helse.ws.inntekt.dto.VirksomhetDTO

object InntektDtoMapper {

    fun toDto(virksomhet: Virksomhet) = VirksomhetDTO(virksomhet.identifikator, virksomhet.type())

    fun toDto(inntekt: Inntekt) =
            InntektDTO(
                arbeidsgiver = toDto(inntekt.virksomhet),
                utbetalingsperiode = inntekt.utbetalingsperiode,
                beløp = inntekt.beløp,
                ytelse = inntekt.isYtelse(),
                kode = inntekt.kode())
}
