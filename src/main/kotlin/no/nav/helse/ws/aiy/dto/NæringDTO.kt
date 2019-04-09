package no.nav.helse.ws.aiy.dto

import no.nav.helse.ws.inntekt.domain.Virksomhet
import java.math.BigDecimal
import java.time.YearMonth

data class NæringDTO(val virksomhet: Virksomhet, val utbetalingsperiode: YearMonth, val beløp: BigDecimal, val kode: String)
