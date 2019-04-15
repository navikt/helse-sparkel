package no.nav.helse.http.aktør

import arrow.core.Either
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.sts.StsRestClient
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue

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

        val lookupResult = aktørregisterClient.gjeldendeIdenter("12345678911")

        assertTrue(lookupResult is Either.Right)
        val gjeldendeIdenter = (lookupResult as Either.Right).b

        Assertions.assertEquals(2, gjeldendeIdenter.size)
        Assertions.assertEquals("1573082186699", gjeldendeIdenter.first { it.type == IdentType.AktoerId }.ident)
        Assertions.assertEquals("12345678911", gjeldendeIdenter.first { it.type == IdentType.NorskIdent }.ident)
    }

    @Test
    fun `should return gjeldende identer by aktoerId`() {
        WireMock.stubFor(aktørregisterRequestMapping
                .withHeader("Nav-Personidenter", WireMock.equalTo("1573082186699"))
                .willReturn(WireMock.ok(ok_aktoerId_response)))

        val lookupResult = aktørregisterClient.gjeldendeIdenter("1573082186699")

        assertTrue(lookupResult is Either.Right)
        val gjeldendeIdenter = (lookupResult as Either.Right).b

        Assertions.assertEquals(2, gjeldendeIdenter.size)
        Assertions.assertEquals("1573082186699", gjeldendeIdenter.first { it.type == IdentType.AktoerId }.ident)
        Assertions.assertEquals("12345678911", gjeldendeIdenter.first { it.type == IdentType.NorskIdent }.ident)
    }

    @Test
    fun `should return gjeldende aktørId by norskIdent`() {
        WireMock.stubFor(aktørregisterRequestMapping
                .withHeader("Nav-Personidenter", WireMock.equalTo("12345678911"))
                .willReturn(WireMock.ok(ok_norskIdent_response)))

        val lookupResult = aktørregisterClient.gjeldendeAktørId("12345678911")

        assertTrue(lookupResult is Either.Right)
        val gjeldendeIdent = (lookupResult as Either.Right).b

        Assertions.assertEquals("1573082186699", gjeldendeIdent)
    }

    @Test
    fun `should return gjeldende norsk ident by aktoerId`() {
        WireMock.stubFor(aktørregisterRequestMapping
                .withHeader("Nav-Personidenter", WireMock.equalTo("1573082186699"))
                .willReturn(WireMock.ok(ok_aktoerId_response)))

        val lookupResult = aktørregisterClient.gjeldendeNorskIdent("1573082186699")

        assertTrue(lookupResult is Either.Right)
        val gjeldendeIdent = (lookupResult as Either.Right).b

        Assertions.assertEquals("12345678911", gjeldendeIdent)
    }

    @Test
    fun `should return feil when ident does not exist`() {
        WireMock.stubFor(aktørregisterRequestMapping
                .withHeader("Nav-Personidenter", WireMock.equalTo("11987654321"))
                .willReturn(WireMock.ok(id_not_found_response)))

        val lookupResult = aktørregisterClient.gjeldendeNorskIdent("11987654321")

        assertTrue(lookupResult is Either.Left)
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

private val id_not_found_response = """
{
    "11987654321": {
        "identer": null,
        "feilmelding": "Den angitte personidenten finnes ikke"
    }
}
""".trimIndent()
