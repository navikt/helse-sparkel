package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.*

class AktørregisterClientTest {

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

    private val aktørregisterClient: AktørregisterClient
    init {
        val stsRestClientMock = mockk<StsRestClient>()
        every {
            stsRestClientMock.token()
        } returns "foobar"

        aktørregisterClient = AktørregisterClient(baseUrl = server.baseUrl(), stsRestClient = stsRestClientMock)
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }

    @Test
    fun `should return gjeldende identer by norskIdent`() {
        WireMock.stubFor(aktørregisterRequestMapping
                .withHeader("Nav-Personidenter", WireMock.equalTo("12345678911"))
                .willReturn(WireMock.ok(ok_norskIdent_response)))

        val gjeldendeIdenter = aktørregisterClient.gjeldendeIdenter("12345678911")

        Assertions.assertEquals(2, gjeldendeIdenter.size)
        Assertions.assertEquals("1573082186699", gjeldendeIdenter.first { it.type == IdentType.AktoerId }.ident)
        Assertions.assertEquals("12345678911", gjeldendeIdenter.first { it.type == IdentType.NorskIdent }.ident)
    }

    @Test
    fun `should return gjeldende identer by aktoerId`() {
        WireMock.stubFor(aktørregisterRequestMapping
                .withHeader("Nav-Personidenter", WireMock.equalTo("1573082186699"))
                .willReturn(WireMock.ok(ok_aktoerId_response)))

        val gjeldendeIdenter = aktørregisterClient.gjeldendeIdenter("1573082186699")

        Assertions.assertEquals(2, gjeldendeIdenter.size)
        Assertions.assertEquals("1573082186699", gjeldendeIdenter.first { it.type == IdentType.AktoerId }.ident)
        Assertions.assertEquals("12345678911", gjeldendeIdenter.first { it.type == IdentType.NorskIdent }.ident)
    }

    @Test
    fun `should return gjeldende aktørId by norskIdent`() {
        WireMock.stubFor(aktørregisterRequestMapping
                .withHeader("Nav-Personidenter", WireMock.equalTo("12345678911"))
                .willReturn(WireMock.ok(ok_norskIdent_response)))

        val gjeldendeIdent = aktørregisterClient.gjeldendeAktørId("12345678911")

        Assertions.assertEquals("1573082186699", gjeldendeIdent)
    }

    @Test
    fun `should return gjeldende norsk ident by aktoerId`() {
        WireMock.stubFor(aktørregisterRequestMapping
                .withHeader("Nav-Personidenter", WireMock.equalTo("1573082186699"))
                .willReturn(WireMock.ok(ok_aktoerId_response)))

        val gjeldendeIdent = aktørregisterClient.gjeldendeNorskIdent("1573082186699")

        Assertions.assertEquals("12345678911", gjeldendeIdent)
    }
}

private val aktørregisterRequestMapping = WireMock.get(WireMock.urlPathEqualTo("/api/v1/identer"))
        .withQueryParam("gjeldende", WireMock.equalTo("true"))
        .withHeader("Authorization", WireMock.equalTo("Bearer foobar"))
        .withHeader("Nav-Call-Id", WireMock.equalTo("anything"))
        .withHeader("Nav-Consumer-Id", WireMock.equalTo("sparkel"))
        .withHeader("Accept", WireMock.equalTo("application/json"))

private val ok_norskIdent_response = """
{
  "12345678911": {
    "identer": [
      {
        "ident": "12345678911",
        "identgruppe": "NorskIdent",
        "gjeldende": true
      },
      {
        "ident": "1573082186699",
        "identgruppe": "AktoerId",
        "gjeldende": true
      }
    ],
    "feilmelding": null
  }
}""".trimIndent()

private val ok_aktoerId_response = """
{
  "1573082186699": {
    "identer": [
      {
        "ident": "1573082186699",
        "identgruppe": "AktoerId",
        "gjeldende": true
      },
      {
        "ident": "12345678911",
        "identgruppe": "NorskIdent",
        "gjeldende": true
      }
    ],
    "feilmelding": null
  }
}""".trimIndent()
