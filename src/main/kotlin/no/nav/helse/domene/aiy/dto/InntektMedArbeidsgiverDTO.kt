package no.nav.helse.domene.aiy.dto

import no.nav.helse.domene.inntekt.dto.VirksomhetDTO
import java.math.BigDecimal

data class InntektMedArbeidsgiverDTO(val arbeidsgiver: VirksomhetDTO, val beløp: BigDecimal)
