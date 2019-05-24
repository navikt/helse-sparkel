package no.nav.helse.domene.ytelse.domain

import no.nav.helse.domene.ytelse.domain.Tema.*
import no.nav.helse.domene.ytelse.domain.Tema.Companion.fraKode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TemaTest {

    @Test
    fun `skal mappe kodeverdier`() {
        assertEquals(Sykepenger, fraKode("SP"))
        assertEquals(Foreldrepenger, fraKode("FA"))
        assertEquals(PårørendeSykdom, fraKode("BS"))
        assertEquals(EnsligForsørger, fraKode("EF"))
        assertTrue(fraKode("ZZ") is Ukjent)
    }

    @Test
    fun `skal ha riktig string-representasjon`() {
        assertEquals("Sykepenger", fraKode("SP").toString())
        assertEquals("Foreldrepenger", fraKode("FA").toString())
        assertEquals("PårørendeSykdom", fraKode("BS").toString())
        assertEquals("EnsligForsørger", fraKode("EF").toString())
        assertEquals("Ukjent(tema=ZZ)", fraKode("ZZ").toString())
    }
}
