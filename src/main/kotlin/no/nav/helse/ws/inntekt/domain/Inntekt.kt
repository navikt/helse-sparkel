package no.nav.helse.ws.inntekt.domain

import java.math.BigDecimal
import java.time.YearMonth

data class Inntekt(val virksomhet: Virksomhet, val utbetalingsperiode: YearMonth, val beløp: BigDecimal)
