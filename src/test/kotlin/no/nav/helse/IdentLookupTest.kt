package no.nav.helse

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.http.aktør.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class IdentLookupTest {

    @Test
    fun `cache should be populated when empty`() {
        val aktørregisterClientMock = mockk<AktørregisterClient>()
        val identCacheMock = mockk<IdentCache>()

        val ident = Ident("123456", IdentType.NorskIdent)
        val identer = listOf(
                Ident("123456", IdentType.NorskIdent),
                Ident("654321", IdentType.AktoerId)
        )

        every {
            aktørregisterClientMock.gjeldendeIdenter("123456")
        } returns identer

        every {
            identCacheMock.fromIdent(ident)
        } returns null

        every {
            identCacheMock.setIdenter(identer)
        } returns "a uuid"

        val lookup = IdentLookup({aktørregisterClientMock}, identCacheMock)

        Assertions.assertEquals("a uuid", lookup.fromIdent(ident))

        verify {
            identCacheMock.setIdenter(identer)
        }
    }

    @Test
    fun `uuid from cache should be returned when cache exists`() {
        val aktørregisterClientMock = mockk<AktørregisterClient>()
        val identCacheMock = mockk<IdentCache>()

        val ident = Ident("123456", IdentType.NorskIdent)

        every {
            identCacheMock.fromIdent(ident)
        } returns "a uuid"

        val lookup = IdentLookup({aktørregisterClientMock}, identCacheMock)

        Assertions.assertEquals("a uuid", lookup.fromIdent(ident))
    }
}
