package no.nav.helse.domene.utbetaling.dto

import java.math.BigDecimal
import java.time.YearMonth

data class UtbetalingEllerTrekkDTO(val virksomhet: VirksomhetDTO,
                                   val utbetalingsperiode: YearMonth,
                                   val beløp: BigDecimal,
                                   val type: String,
                                   val ytelse: Boolean,
                                   val kode: String?)
