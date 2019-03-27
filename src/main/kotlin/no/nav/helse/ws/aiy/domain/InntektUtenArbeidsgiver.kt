package no.nav.helse.ws.aiy.domain

import java.math.BigDecimal
import java.time.YearMonth

data class InntektUtenArbeidsgiver(val utbetalingsperiode: YearMonth, val beløp: BigDecimal)
