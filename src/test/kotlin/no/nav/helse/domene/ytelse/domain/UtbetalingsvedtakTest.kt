package no.nav.helse.domene.ytelse.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtbetalingsvedtakTest {

    @Test
    fun `fom kan ikke være nyere enn tom`() {
        assertThrows(IllegalArgumentException::class.java) {
            skalIkkeUtbetales().medTom(LocalDate.now().minusMonths(1))
        }

        assertThrows(IllegalArgumentException::class.java) {
            skalUtbetales().medTom(LocalDate.now().minusMonths(1))
        }
    }

    @Test
    fun `skalIkkeUtbetales er lik objekt med samme type og verdi`() {
        assertEquals(skalIkkeUtbetales(), skalIkkeUtbetales())
    }

    @Test
    fun `skalUtbetales er lik objekt med samme type og verdi`() {
        assertEquals(skalUtbetales(), skalUtbetales())
    }

    @Test
    fun `skalUtbetales er ulik skalIkkeUtbetales`() {
        val fom = LocalDate.now().minusDays(1)
        val tom = LocalDate.now()

        assertNotEquals(skalIkkeUtbetales().medFom(fom).medTom(tom), skalUtbetales().medFom(fom).medTom(tom))
    }

    @Test
    fun `skalIkkeUtbetales er ulik objekt med samme type, men ulike verdier`() {
        assertNotEquals(skalIkkeUtbetales(), skalIkkeUtbetales().medTom(LocalDate.now().plusDays(1)))
        assertNotEquals(skalIkkeUtbetales(), skalIkkeUtbetales().medFom(LocalDate.now().minusMonths(1)))
    }

    @Test
    fun `skalUtbetales er ulik objekt med samme type, men ulike verdier`() {
        assertNotEquals(skalUtbetales(), skalUtbetales().medTom(LocalDate.now().plusDays(1)))
        assertNotEquals(skalUtbetales(), skalUtbetales().medFom(LocalDate.now().minusMonths(1)))
        assertNotEquals(skalUtbetales(), skalUtbetales().medUtbetalingsgrad(50))
    }

    @Test
    fun `string-representasjon av skalUtbetales`() {
        val fom = LocalDate.of(2019, 1, 1)
        val tom = LocalDate.of(2019, 2, 1)

        assertEquals("Utbetalingsvedtak.SkalUtbetales(fom=2019-01-01, tom=2019-02-01, utbetalingsgrad=100)", skalUtbetales().medFom(fom).medTom(tom).toString())
    }

    @Test
    fun `string-representasjon av skalIkkeUtbetales`() {
        val fom = LocalDate.of(2019, 1, 1)
        val tom = LocalDate.of(2019, 2, 1)

        assertEquals("Utbetalingsvedtak.SkalIkkeUtbetales(fom=2019-01-01, tom=2019-02-01)", skalIkkeUtbetales().medFom(fom).medTom(tom).toString())
    }

    private fun skalIkkeUtbetales() =
            Utbetalingsvedtak.SkalIkkeUtbetales(
                    fom = LocalDate.now().minusDays(14),
                    tom = LocalDate.now()
            )

    private fun skalUtbetales() =
            Utbetalingsvedtak.SkalUtbetales(
                    fom = LocalDate.now().minusDays(14),
                    tom = LocalDate.now(),
                    utbetalingsgrad = 100
            )

    private fun Utbetalingsvedtak.SkalIkkeUtbetales.medFom(fom: LocalDate) =
            Utbetalingsvedtak.SkalIkkeUtbetales(
                    fom = fom,
                    tom = tom
            )
    private fun Utbetalingsvedtak.SkalIkkeUtbetales.medTom(tom: LocalDate) =
            Utbetalingsvedtak.SkalIkkeUtbetales(
                    fom = fom,
                    tom = tom
            )

    private fun Utbetalingsvedtak.SkalUtbetales.medFom(fom: LocalDate) =
            Utbetalingsvedtak.SkalUtbetales(
                    fom = fom,
                    tom = tom,
                    utbetalingsgrad = utbetalingsgrad
            )

    private fun Utbetalingsvedtak.SkalUtbetales.medTom(tom: LocalDate) =
            Utbetalingsvedtak.SkalUtbetales(
                    fom = fom,
                    tom = tom,
                    utbetalingsgrad = utbetalingsgrad
            )

    private fun Utbetalingsvedtak.SkalUtbetales.medUtbetalingsgrad(utbetalingsgrad: Int) =
            Utbetalingsvedtak.SkalUtbetales(
                    fom = fom,
                    tom = tom,
                    utbetalingsgrad = utbetalingsgrad
            )
}
