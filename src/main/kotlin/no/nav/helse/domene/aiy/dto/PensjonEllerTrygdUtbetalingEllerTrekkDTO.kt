package no.nav.helse.domene.aiy.dto

import no.nav.helse.domene.utbetaling.dto.VirksomhetDTO
import java.math.BigDecimal
import java.time.YearMonth

data class PensjonEllerTrygdUtbetalingEllerTrekkDTO(val virksomhet: VirksomhetDTO, val utbetalingsperiode: YearMonth, val beløp: BigDecimal, val kode: String)
