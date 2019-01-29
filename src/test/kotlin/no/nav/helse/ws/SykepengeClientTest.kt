package no.nav.helse.ws

import io.mockk.mockk
import no.nav.helse.common.toLocalDate
import no.nav.helse.ws.sykepenger.SykepengerClient
import no.nav.tjeneste.virksomhet.sykepenger.v2.binding.SykepengerV2
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class SykepengeClientTest {

    @Test
    fun `valid request transforms`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now()
        val fnr = "12345678901 "

        val mock = mockk<SykepengerV2>()
        val client = SykepengerClient(mock)
        val request = client.createSykepengerListeRequest(fnr, fom, tom)

        assertEquals(request.ident, fnr)


        assertTrue(request.sykmelding.fom.toLocalDate().compareTo(fom) == 0)
        assertTrue(request.sykmelding.tom.toLocalDate().compareTo(tom) == 0)

    }
}
