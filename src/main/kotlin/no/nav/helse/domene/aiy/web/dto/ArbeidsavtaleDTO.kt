package no.nav.helse.domene.aiy.web.dto

import java.math.BigDecimal
import java.time.LocalDate

data class ArbeidsavtaleDTO(val yrke: String,
                            val stillingsprosent: BigDecimal?,
                            val fom: LocalDate,
                            val tom: LocalDate?)
