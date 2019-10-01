package no.nav.helse.oppslag.spole

import arrow.core.getOrElse
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.domene.AktørId
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SpoleClientTest {

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    private val spoleClient: SpoleClient

    init {
        val azureClientMock = mockk<AzureClient>()
        every {
            azureClientMock.fetchToken()
        } returns AzureClient.Token(
                tokenType = "Bearer",
                expiresIn = 3600,
                accessToken = "foobar"
        )

        spoleClient = SpoleClient(baseUrl = server.baseUrl(), azureClient = azureClientMock)
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }

    @Test
    fun `henter sykepengeperioder fra spole`() {
        WireMock.stubFor(spoleRequestMapping
                .willReturn(WireMock.ok(ok_sykepengeperioder_response)))

        val lookupResult = spoleClient.hentSykepengeperioder(aktørId = AktørId(aktørId))

        assertTrue(lookupResult.isSuccess())

        val sykepengeperioder = lookupResult.getOrElse { throw RuntimeException("expected a value") }

        assertEquals(1, sykepengeperioder.perioder.size)
        assertEquals(LocalDate.parse("2019-01-01"), sykepengeperioder.perioder[0].fom)
        assertEquals(LocalDate.parse("2019-02-01"), sykepengeperioder.perioder[0].tom)
        assertEquals("100", sykepengeperioder.perioder[0].grad)
    }
}

private val aktørId = "123456789123"

private val spoleRequestMapping = WireMock.get(WireMock.urlPathEqualTo("/sykepengeperioder/$aktørId"))
        .withHeader("Authorization", WireMock.equalTo("Bearer foobar"))
        .withHeader("Nav-Call-Id", WireMock.equalTo("anything"))
        .withHeader("Accept", WireMock.equalTo("application/json"))

private val ok_sykepengeperioder_response = """
{
    "aktørId": "$aktørId",
    "perioder": [
        {
            "fom": "2019-01-01",
            "tom": "2019-02-01",
            "grad": "100"
        }   
    ]
}
}""".trimIndent()

