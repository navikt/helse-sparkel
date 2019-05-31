package no.nav.helse.domene.aiy.domain

import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjonsnummer
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ArbeidsforholdTest {

    @Test
    fun `arbeidstaker må ha minst én arbeidsavtale`() {
        assertThrows(IllegalArgumentException::class.java) {
            Arbeidsforhold.Arbeidstaker(
                    arbeidsgiver = Virksomhet.Organisasjon(Organisasjonsnummer("995298775")),
                    arbeidsforholdId = 1234L,
                    startdato = LocalDate.now(),
                    arbeidsavtaler = emptyList()
            )
        }
    }

    @Test
    fun `arbeidstaker må ha minst én gjeldende arbeidsavtale`() {
        assertThrows(IllegalArgumentException::class.java) {
            Arbeidsforhold.Arbeidstaker(
                    arbeidsgiver = Virksomhet.Organisasjon(Organisasjonsnummer("995298775")),
                    arbeidsforholdId = 1234L,
                    startdato = LocalDate.now(),
                    arbeidsavtaler = listOf(
                            Arbeidsavtale.Historisk(
                                    yrke = "BUTIKKMEDARBEIDER",
                                    stillingsprosent = BigDecimal(100),
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now()
                            )
                    )
            )
        }
    }

    @Test
    fun `arbeidstaker må ha én gjeldende arbeidsavtale`() {
        assertThrows(IllegalArgumentException::class.java) {
            Arbeidsforhold.Arbeidstaker(
                    arbeidsgiver = Virksomhet.Organisasjon(Organisasjonsnummer("995298775")),
                    arbeidsforholdId = 1234L,
                    startdato = LocalDate.now(),
                    arbeidsavtaler = listOf(
                            Arbeidsavtale.Gjeldende(
                                    yrke = "BUTIKKMEDARBEIDER",
                                    stillingsprosent = BigDecimal(100),
                                    fom = LocalDate.now()
                            ),
                            Arbeidsavtale.Gjeldende(
                                    yrke = "BUTIKKMEDARBEIDER",
                                    stillingsprosent = BigDecimal(100),
                                    fom = LocalDate.now()
                            )
                    )
            )
        }
    }
}
