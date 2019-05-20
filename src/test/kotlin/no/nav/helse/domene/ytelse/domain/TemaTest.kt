package no.nav.helse.domene.ytelse.domain

import no.nav.helse.domene.ytelse.domain.Tema.*
import no.nav.helse.domene.ytelse.domain.Tema.Companion.fraKode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TemaTest {

    @Test
    fun `skal mappe kodeverdier`() {
        assertEquals(Sykepenger, fraKode("SP"))
        assertEquals(Foreldrepenger, fraKode("FA"))
        assertEquals(PårørendeSykdom, fraKode("BS"))
        assertEquals(EnsligForsørger, fraKode("EF"))
    }
}
