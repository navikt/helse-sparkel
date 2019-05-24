package no.nav.helse.domene.arbeid.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ArbeidsavtaleTest {

    @Test
    fun `gjeldende arbeidsavtale er ulik en historisk avtale`() {
        val avtale1 = enGjeldendeArbeidsavtale()
        val avtale2 = enHistoriskArbeidsavtale()

        assertNotEquals(avtale1, avtale2)
        assertNotEquals(avtale1.hashCode(), avtale2.hashCode())
    }

    @Test
    fun `gjeldende arbeidsavtale er lik en annen med samme verdier`() {
        val avtale1 = enGjeldendeArbeidsavtale()
        val avtale2 = enGjeldendeArbeidsavtale()

        assertEquals(avtale1, avtale2)
        assertEquals(avtale1.hashCode(), avtale2.hashCode())
    }

    @Test
    fun `gjeldende arbeidsavtale er ulik en annen med ulike verdier`() {
        val avtale1 = enGjeldendeArbeidsavtale()
        val avtale2 = enGjeldendeArbeidsavtale()
                .medYrke("SJEF")

        assertNotEquals(avtale1, avtale2)
        assertNotEquals(avtale1.hashCode(), avtale2.hashCode())

        val avtale3 = enGjeldendeArbeidsavtale()
                .medStillingsprosent(BigDecimal.ONE)

        assertNotEquals(avtale1, avtale3)
        assertNotEquals(avtale1.hashCode(), avtale3.hashCode())

        val avtale4 = enGjeldendeArbeidsavtale()
                .medFom(LocalDate.now().minusMonths(1))

        assertNotEquals(avtale1, avtale4)
        assertNotEquals(avtale1.hashCode(), avtale4.hashCode())
    }

    @Test
    fun `historisk arbeidsavtale er lik en annen med samme verdier`() {
        val avtale1 = enHistoriskArbeidsavtale()
        val avtale2 = enHistoriskArbeidsavtale()

        assertEquals(avtale1, avtale2)
        assertEquals(avtale1.hashCode(), avtale2.hashCode())
    }

    @Test
    fun `historisk arbeidsavtale er ulik en annen med ulike verdier`() {
        val avtale1 = enHistoriskArbeidsavtale()
        val avtale2 = enHistoriskArbeidsavtale()
                .medYrke("SJEF")

        assertNotEquals(avtale1, avtale2)
        assertNotEquals(avtale1.hashCode(), avtale2.hashCode())

        val avtale3 = enHistoriskArbeidsavtale()
                .medStillingsprosent(BigDecimal.ONE)

        assertNotEquals(avtale1, avtale3)
        assertNotEquals(avtale1.hashCode(), avtale3.hashCode())

        val avtale4 = enHistoriskArbeidsavtale()
                .medFom(LocalDate.now().minusDays(1))

        assertNotEquals(avtale1, avtale4)
        assertNotEquals(avtale1.hashCode(), avtale4.hashCode())

        val avtale5 = enHistoriskArbeidsavtale()
                .medTom(LocalDate.now().minusMonths(1))

        assertNotEquals(avtale1, avtale5)
        assertNotEquals(avtale1.hashCode(), avtale5.hashCode())
    }

    private fun enGjeldendeArbeidsavtale() =
            Arbeidsavtale.Gjeldende(
                    yrke = "BUTIKKMEDARBEIDER",
                    stillingsprosent = BigDecimal(100),
                    fom = LocalDate.now().minusDays(1)
            )

    private fun enHistoriskArbeidsavtale() =
            Arbeidsavtale.Historisk(
                    yrke = "BUTIKKMEDARBEIDER",
                    stillingsprosent = BigDecimal(100),
                    fom = LocalDate.now().minusDays(1),
                    tom = LocalDate.now()
            )

    private fun Arbeidsavtale.Gjeldende.medYrke(yrke: String) =
            Arbeidsavtale.Gjeldende(
                    yrke = yrke,
                    stillingsprosent = stillingsprosent,
                    fom = fom
            )
    private fun Arbeidsavtale.Gjeldende.medStillingsprosent(stillingsprosent: BigDecimal) =
            Arbeidsavtale.Gjeldende(
                    yrke = yrke,
                    stillingsprosent = stillingsprosent,
                    fom = fom
            )
    private fun Arbeidsavtale.Gjeldende.medFom(fom: LocalDate) =
            Arbeidsavtale.Gjeldende(
                    yrke = yrke,
                    stillingsprosent = stillingsprosent,
                    fom = fom
            )

    private fun Arbeidsavtale.Historisk.medYrke(yrke: String) =
            Arbeidsavtale.Historisk(
                    yrke = yrke,
                    stillingsprosent = stillingsprosent,
                    fom = fom,
                    tom = fom
            )
    private fun Arbeidsavtale.Historisk.medStillingsprosent(stillingsprosent: BigDecimal) =
            Arbeidsavtale.Historisk(
                    yrke = yrke,
                    stillingsprosent = stillingsprosent,
                    fom = fom,
                    tom = fom
            )
    private fun Arbeidsavtale.Historisk.medFom(fom: LocalDate) =
            Arbeidsavtale.Historisk(
                    yrke = yrke,
                    stillingsprosent = stillingsprosent,
                    fom = fom,
                    tom = fom
            )
    private fun Arbeidsavtale.Historisk.medTom(tom: LocalDate) =
            Arbeidsavtale.Historisk(
                    yrke = yrke,
                    stillingsprosent = stillingsprosent,
                    fom = fom,
                    tom = fom
            )
}
