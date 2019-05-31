package no.nav.helse.domene.aiy.web.dto

import no.nav.helse.domene.aiy.web.dto.VirksomhetDTO
import java.math.BigDecimal
import java.time.YearMonth

data class LønnsutbetalingEllerTrekkDTO(val virksomhet: VirksomhetDTO,
                                        val utbetalingsperiode: YearMonth,
                                        val beløp: BigDecimal)
