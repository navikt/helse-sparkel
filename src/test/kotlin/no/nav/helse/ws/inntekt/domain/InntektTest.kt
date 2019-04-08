package no.nav.helse.ws.inntekt.domain

import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InntektTest {

    @Test
    fun `skal angi type`() {
        val orgnr = Organisasjonsnummer("889640782")
        val virksomhet = Virksomhet.Organisasjon(orgnr)
        val utbetalingsperiode = YearMonth.of(2019, 1)
        val beløp = BigDecimal(1500)

        assertEquals("Lønn", Inntekt.Lønn(virksomhet, utbetalingsperiode, beløp).type())
        assertEquals("Ytelse", Inntekt.Ytelse(virksomhet, utbetalingsperiode, beløp, "barnetrygd").type())
        assertEquals("PensjonEllerTrygd", Inntekt.PensjonEllerTrygd(virksomhet, utbetalingsperiode, beløp, "alderspensjon").type())
        assertEquals("Næring", Inntekt.Næring(virksomhet, utbetalingsperiode, beløp, "næring").type())
    }

    @Test
    fun `skal angi kode`() {
        val orgnr = Organisasjonsnummer("889640782")
        val virksomhet = Virksomhet.Organisasjon(orgnr)
        val utbetalingsperiode = YearMonth.of(2019, 1)
        val beløp = BigDecimal(1500)

        assertEquals(null, Inntekt.Lønn(virksomhet, utbetalingsperiode, beløp).kode())
        assertEquals("barnetrygd", Inntekt.Ytelse(virksomhet, utbetalingsperiode, beløp, "barnetrygd").kode())
        assertEquals("alderspensjon", Inntekt.PensjonEllerTrygd(virksomhet, utbetalingsperiode, beløp, "alderspensjon").kode())
        assertEquals("næring", Inntekt.Næring(virksomhet, utbetalingsperiode, beløp, "næring").kode())
    }

    @Test
    fun `skal angi ytelse`() {
        val orgnr = Organisasjonsnummer("889640782")
        val virksomhet = Virksomhet.Organisasjon(orgnr)
        val utbetalingsperiode = YearMonth.of(2019, 1)
        val beløp = BigDecimal(1500)

        assertFalse(Inntekt.Lønn(virksomhet, utbetalingsperiode, beløp).isYtelse())
        assertTrue(Inntekt.Ytelse(virksomhet, utbetalingsperiode, beløp, "barnetrygd").isYtelse())
        assertTrue(Inntekt.PensjonEllerTrygd(virksomhet, utbetalingsperiode, beløp, "alderspensjon").isYtelse())
        assertFalse(Inntekt.Næring(virksomhet, utbetalingsperiode, beløp, "næring").isYtelse())
    }
}
