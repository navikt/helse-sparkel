package no.nav.helse.ws.inntekt.dto

import java.math.BigDecimal
import java.time.YearMonth

data class InntektDTO(val arbeidsgiver: ArbeidsgiverDTO, val utbetalingsperiode: YearMonth, val beløp: BigDecimal, val ytelse: Boolean, val kode: String?)
