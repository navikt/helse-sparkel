package no.nav.helse.domene.aiy.inntektskomponenten

import no.nav.helse.domene.aiy.domain.UtbetalingEllerTrekk
import no.nav.helse.domene.aiy.domain.Virksomhet
import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjonsnummer
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

        assertEquals("Lønn", UtbetalingEllerTrekk.Lønn(virksomhet, utbetalingsperiode, beløp).type())
        assertEquals("Ytelse", UtbetalingEllerTrekk.Ytelse(virksomhet, utbetalingsperiode, beløp, "barnetrygd").type())
        assertEquals("PensjonEllerTrygd", UtbetalingEllerTrekk.PensjonEllerTrygd(virksomhet, utbetalingsperiode, beløp, "alderspensjon").type())
        assertEquals("Næring", UtbetalingEllerTrekk.Næring(virksomhet, utbetalingsperiode, beløp, "næring").type())
    }

    @Test
    fun `skal angi kode`() {
        val orgnr = Organisasjonsnummer("889640782")
        val virksomhet = Virksomhet.Organisasjon(orgnr)
        val utbetalingsperiode = YearMonth.of(2019, 1)
        val beløp = BigDecimal(1500)

        assertEquals(null, UtbetalingEllerTrekk.Lønn(virksomhet, utbetalingsperiode, beløp).kode())
        assertEquals("barnetrygd", UtbetalingEllerTrekk.Ytelse(virksomhet, utbetalingsperiode, beløp, "barnetrygd").kode())
        assertEquals("alderspensjon", UtbetalingEllerTrekk.PensjonEllerTrygd(virksomhet, utbetalingsperiode, beløp, "alderspensjon").kode())
        assertEquals("næring", UtbetalingEllerTrekk.Næring(virksomhet, utbetalingsperiode, beløp, "næring").kode())
    }

    @Test
    fun `skal angi ytelse`() {
        val orgnr = Organisasjonsnummer("889640782")
        val virksomhet = Virksomhet.Organisasjon(orgnr)
        val utbetalingsperiode = YearMonth.of(2019, 1)
        val beløp = BigDecimal(1500)

        assertFalse(UtbetalingEllerTrekk.Lønn(virksomhet, utbetalingsperiode, beløp).isYtelse())
        assertTrue(UtbetalingEllerTrekk.Ytelse(virksomhet, utbetalingsperiode, beløp, "barnetrygd").isYtelse())
        assertTrue(UtbetalingEllerTrekk.PensjonEllerTrygd(virksomhet, utbetalingsperiode, beløp, "alderspensjon").isYtelse())
        assertFalse(UtbetalingEllerTrekk.Næring(virksomhet, utbetalingsperiode, beløp, "næring").isYtelse())
    }
}
