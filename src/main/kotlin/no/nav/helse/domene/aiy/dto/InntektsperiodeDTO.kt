package no.nav.helse.domene.aiy.dto

import java.math.BigDecimal

data class InntektsperiodeDTO(val sum: BigDecimal, val inntekter: List<InntektDTO>)
