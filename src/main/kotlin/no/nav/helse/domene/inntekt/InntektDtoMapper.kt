package no.nav.helse.domene.inntekt

import no.nav.helse.domene.inntekt.domain.Inntekt
import no.nav.helse.domene.inntekt.domain.Virksomhet
import no.nav.helse.domene.inntekt.dto.InntektDTO
import no.nav.helse.domene.inntekt.dto.VirksomhetDTO

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
