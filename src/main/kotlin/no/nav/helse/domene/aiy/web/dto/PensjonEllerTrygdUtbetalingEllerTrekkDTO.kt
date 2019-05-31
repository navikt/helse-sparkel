package no.nav.helse.domene.aiy.web.dto

import no.nav.helse.domene.aiy.web.dto.VirksomhetDTO
import java.math.BigDecimal
import java.time.YearMonth

data class PensjonEllerTrygdUtbetalingEllerTrekkDTO(val virksomhet: VirksomhetDTO, val utbetalingsperiode: YearMonth, val beløp: BigDecimal, val kode: String)
