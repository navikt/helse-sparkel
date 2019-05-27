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

        val gjeldendeArbeidsavtale: Arbeidsavtale.Gjeldende

        init {
            val antallGjeldendeArbeidsavtaler = arbeidsavtaler.filter { it is Arbeidsavtale.Gjeldende }.size

            if (antallGjeldendeArbeidsavtaler != 1) {
                throw IllegalArgumentException("et arbeidsforhold må ha én gjeldende arbeidsavtale: $antallGjeldendeArbeidsavtaler gjeldende avtaler er ikke mulig")
            }

            gjeldendeArbeidsavtale = arbeidsavtaler.first { it is Arbeidsavtale.Gjeldende } as Arbeidsavtale.Gjeldende
        }

        fun yrke() = gjeldendeArbeidsavtale.yrke
    }

    data class Frilans(
            override val arbeidsgiver: Virksomhet,
            override val startdato: LocalDate,
            override val sluttdato: LocalDate? = null,
            val yrke: String): Arbeidsforhold(arbeidsgiver, startdato, sluttdato)
}
