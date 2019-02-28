package no.nav.helse.ws.sakogbehandling

import io.mockk.*
import no.nav.helse.*
import no.nav.helse.ws.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class SakOgBehandlingServiceTest {

    @Test
    fun `oppslag-exceptions mappes til feil`() {
        val client = mockk<SakOgBehandlingClient>()
        val service = SakOgBehandlingService(client)
        every {
            client.finnSakOgBehandling("1234")
        } returns Either.Left(Exception("well that didn't work now, did it?"))

        val result = service.finnSakOgBehandling(AktørId("1234"))
        assertTrue(result is Either.Left)
    }

    @Test
    fun `fungerende oppslg mappes til ok`() {
        val client = mockk<SakOgBehandlingClient>()
        val service = SakOgBehandlingService(client)
        every {
            client.finnSakOgBehandling("1234")
        } returns Either.Right(emptyList())

        val result = service.finnSakOgBehandling(AktørId("1234"))
        assertTrue(result is Either.Right)
    }

}
