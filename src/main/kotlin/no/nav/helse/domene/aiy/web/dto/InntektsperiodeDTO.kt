package no.nav.helse.domene.aiy.web.dto

import java.math.BigDecimal

data class InntektsperiodeDTO(val sum: BigDecimal, val inntekter: List<LønnsutbetalingEllerTrekkDTO>)
