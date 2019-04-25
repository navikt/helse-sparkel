package no.nav.helse.domene.arbeid.domain

import no.nav.helse.domene.inntekt.domain.Virksomhet
import java.time.LocalDate

sealed class Arbeidsforhold(open val arbeidsgiver: Virksomhet, open val startdato: LocalDate, open val sluttdato: LocalDate?) {

    init {
        if (sluttdato != null && startdato > sluttdato) {
            throw IllegalArgumentException("startdato er større enn sluttdato")
        }
    }

    fun type() = when (this) {
        is Arbeidstaker -> "Arbeidstaker"
        is Frilans -> "Frilans"
    }

    data class Arbeidstaker(override val arbeidsgiver: Virksomhet,
                            val arbeidsforholdId: Long,
                            override val startdato: LocalDate,
                            override val sluttdato: LocalDate? = null,
                            val permisjon: List<Permisjon> = emptyList(),
                            val arbeidsavtaler: List<Arbeidsavtale> = emptyList()): Arbeidsforhold(arbeidsgiver, startdato, sluttdato) {
        init {
            if (arbeidsavtaler.isEmpty()) {
                throw IllegalArgumentException("arbeidsavtaler er tom: $arbeidsforholdId")
            }
            if (arbeidsavtaler.filter { it.tom == null }.size > 1) {
                throw IllegalArgumentException("det er flere enn én gjeldende arbeidsavtale")
            }
        }
    }

    data class Frilans(
            override val arbeidsgiver: Virksomhet,
            override val startdato: LocalDate,
            override val sluttdato: LocalDate? = null,
            val yrke: String): Arbeidsforhold(arbeidsgiver, startdato, sluttdato) {
        init {
            if (yrke.isBlank()) {
                throw IllegalArgumentException("yrke kan ikke være en tom streng")
            }
        }
    }
}
