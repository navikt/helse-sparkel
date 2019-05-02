package no.nav.helse.domene.aiy.dto

import no.nav.helse.domene.inntekt.dto.VirksomhetDTO
import java.math.BigDecimal
import java.time.YearMonth

data class NæringDTO(val virksomhet: VirksomhetDTO, val utbetalingsperiode: YearMonth, val beløp: BigDecimal, val kode: String)
