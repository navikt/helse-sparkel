package no.nav.helse.ws.arbeidsforhold.domain

import java.math.BigDecimal
import java.time.LocalDate

data class Arbeidsavtale(val yrke: String,
                         val stillingsprosent: BigDecimal,
                         val fom: LocalDate,
                         val tom: LocalDate?)
