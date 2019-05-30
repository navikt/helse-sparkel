package no.nav.helse.domene.aiy.domain

import java.math.BigDecimal
import java.time.LocalDate

data class Permisjon(val fom: LocalDate,
                     val tom: LocalDate?,
                     val permisjonsprosent: BigDecimal,
                     val årsak: String)
