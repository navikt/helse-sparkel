package no.nav.helse.domene.arbeid.domain

import java.math.BigDecimal
import java.time.LocalDate

data class Arbeidsavtale(val yrke: String,
                         val stillingsprosent: BigDecimal,
                         val fom: LocalDate,
                         val tom: LocalDate?) {
    init {
        if (yrke.isBlank()) {
            throw IllegalArgumentException("yrke kan ikke være en tom streng")
        }
        if (stillingsprosent < BigDecimal.ZERO || stillingsprosent > BigDecimal(100)) {
            throw IllegalArgumentException("stillingsprosent $stillingsprosent må være i intervallet [0, 100]")
        }
        if (tom != null && fom > tom) {
            throw IllegalArgumentException("fom er større enn tom")
        }
    }
}
