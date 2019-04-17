package no.nav.helse.ws.aiy.dto

import no.nav.helse.ws.inntekt.dto.VirksomhetDTO
import java.math.BigDecimal

data class InntektMedArbeidsgiverDTO(val arbeidsgiver: VirksomhetDTO, val beløp: BigDecimal)
