package no.nav.helse.domene.aiy.web.dto

import java.math.BigDecimal
import java.time.LocalDate

data class PermisjonDTO(val fom: LocalDate,
                        val tom: LocalDate?,
                        val permisjonsprosent: BigDecimal,
                        val arsak: String)
