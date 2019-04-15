package no.nav.helse.ws.sakogbehandling

import arrow.core.Either
import arrow.core.Try
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.ws.AktørId
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SakOgBehandlingServiceTest {

    @Test
    fun `oppslag-exceptions mappes til feil`() {
        val client = mockk<SakOgBehandlingClient>()
        val service = SakOgBehandlingService(client)
        every {
            client.finnSakOgBehandling("1234")
        } returns Try.Failure(Exception("well that didn't work now, did it?"))

        val result = service.finnSakOgBehandling(AktørId("1234"))
        assertTrue(result is Either.Left)
    }

    @Test
    fun `fungerende oppslg mappes til ok`() {
        val client = mockk<SakOgBehandlingClient>()
        val service = SakOgBehandlingService(client)
        every {
            client.finnSakOgBehandling("1234")
        } returns Try.Success(emptyList())

        val result = service.finnSakOgBehandling(AktørId("1234"))
        assertTrue(result is Either.Right)
    }

}
