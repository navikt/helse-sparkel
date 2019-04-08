package no.nav.helse.ws.aiy.dto

import java.math.BigDecimal
import java.time.YearMonth

data class InntektUtenArbeidsgiverDTO(val utbetalingsperiode: YearMonth, val beløp: BigDecimal)
