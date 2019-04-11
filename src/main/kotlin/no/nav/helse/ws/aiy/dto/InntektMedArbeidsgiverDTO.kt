package no.nav.helse.ws.aiy.dto

import no.nav.helse.ws.inntekt.dto.ArbeidsgiverDTO
import java.math.BigDecimal

data class InntektMedArbeidsgiverDTO(val arbeidsgiver: ArbeidsgiverDTO, val beløp: BigDecimal)
