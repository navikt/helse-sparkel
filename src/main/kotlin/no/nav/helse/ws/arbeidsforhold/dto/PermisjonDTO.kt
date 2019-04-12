package no.nav.helse.ws.arbeidsforhold.dto

import java.math.BigDecimal
import java.time.LocalDate

data class PermisjonDTO(val fom: LocalDate,
                        val tom: LocalDate,
                        val permisjonsprosent: BigDecimal,
                        val arsak: String)
