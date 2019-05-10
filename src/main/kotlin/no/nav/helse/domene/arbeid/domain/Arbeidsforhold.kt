package no.nav.helse.domene.arbeid.domain

import no.nav.helse.domene.utbetaling.domain.Virksomhet
import java.time.LocalDate

sealed class Arbeidsforhold(open val arbeidsgiver: Virksomhet, open val startdato: LocalDate, open val sluttdato: LocalDate?) {

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

        fun yrke() = gjeldendeArbeidsavtale().yrke

        fun gjeldendeArbeidsavtale() = arbeidsavtaler.first {
            it.tom == null
        }
    }

    data class Frilans(
            override val arbeidsgiver: Virksomhet,
            override val startdato: LocalDate,
            override val sluttdato: LocalDate? = null,
            val yrke: String): Arbeidsforhold(arbeidsgiver, startdato, sluttdato)
}
