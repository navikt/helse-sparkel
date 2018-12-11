package no.nav.helse.ws

import io.mockk.mockk
import no.nav.helse.ws.sykepenger.SykepengerClient
import no.nav.tjeneste.virksomhet.sykepenger.v2.binding.SykepengerV2
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SykepengeClientTest {

    @Test
    fun `valid request transforms`() {
        val fom = DateTime.now()
        val tom = DateTime.now()
        val fnr = "12345678901 "

        val mock = mockk<SykepengerV2>()
        val client = SykepengerClient(mock)
        val request = client.createSykepengerListeRequest(fnr, fom, tom)

        assertEquals(request.ident, fnr)


        assertTrue(request.sykmelding.fom.toGregorianCalendar().compareTo(fom.toGregorianCalendar()) == 0)
        assertTrue(request.sykmelding.tom.toGregorianCalendar().compareTo(tom.toGregorianCalendar()) == 0)

    }
}