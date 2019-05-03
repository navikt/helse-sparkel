package no.nav.helse.domene.arbeid.domain

import java.math.BigDecimal
import java.time.LocalDate

data class Permisjon(val fom: LocalDate,
                     val tom: LocalDate?,
                     val permisjonsprosent: BigDecimal,
                     val årsak: String) {
    init {
        if (permisjonsprosent < BigDecimal.ZERO || permisjonsprosent > BigDecimal(100)) {
            throw IllegalArgumentException("permisjonsprosent $permisjonsprosent må være i intervallet [0, 100]")
        }
        if (tom != null && fom > tom) {
            throw IllegalArgumentException("fom er større enn tom")
        }
    }
}
