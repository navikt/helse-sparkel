package no.nav.helse.ws.aiy.domain

import no.nav.helse.ws.inntekt.domain.Virksomhet
import java.time.LocalDate

sealed class Arbeidsforhold(open val arbeidsgiver: Virksomhet, open val startdato: LocalDate, open val sluttdato: LocalDate?) {

    fun type() = when (this) {
        is Arbeidstaker -> "Arbeidstaker"
        is Frilans -> "Frilans"
    }

    data class Arbeidstaker(override val arbeidsgiver: Virksomhet,
                            override val startdato: LocalDate,
                            override val sluttdato: LocalDate? = null): Arbeidsforhold(arbeidsgiver, startdato, sluttdato)
    data class Frilans(
            override val arbeidsgiver: Virksomhet,
            override val startdato: LocalDate,
            override val sluttdato: LocalDate? = null,
            val yrke: String? = null): Arbeidsforhold(arbeidsgiver, startdato, sluttdato)
}
