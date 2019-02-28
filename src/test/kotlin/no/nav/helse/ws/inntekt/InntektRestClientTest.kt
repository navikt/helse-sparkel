package no.nav.helse.ws.inntekt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.sts.StsRestClient
import org.junit.jupiter.api.*
import java.time.YearMonth

class InntektRestClientTest {

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

    private lateinit var inntektRestClient: InntektRestClient

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())

        val stsRestClient = mockk<StsRestClient>()
        every {
            stsRestClient.token()
        } returns "foobar"

        inntektRestClient = InntektRestClient(baseUrl = server.baseUrl(), stsRestClient = stsRestClient)
    }


    @Test
    fun `should return json`() {
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/api/v1/hentinntektliste"))
                .withHeader("Nav-Consumer-Id", WireMock.equalTo("sparkel"))
                .withHeader("Nav-Call-Id", WireMock.equalTo("anything"))
                .withHeader("Authorization", WireMock.equalTo("Bearer foobar"))
                .withHeader("Accept", WireMock.equalTo("application/json"))
                .withRequestBody(WireMock.equalToJson("""
{
    "ident": {
        "identifikator": "12345678911",
        "aktoerType": "NATURLIG_IDENT"
    },
    "maanedFom": "2018-01"
}""".trimIndent()))
                .willReturn(WireMock.okJson(ok_inntekt_response))
                .inScenario("inntektskomponenten")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("inntektliste hentet"))

        val json = inntektRestClient.hentInntektListe("12345678911", YearMonth.of(2018, 1), InntektRestClient.IdentType.NATURLIG_IDENT)

        // TODO: figure out what an OK response looks like
        Assertions.assertEquals(403, json.getInt("status"))
    }
}

private val ok_inntekt_response = """
{
    "timestamp": "2018-11-16T09:34:15.627+0100",
    "status": "403",
    "error": "Forbidden",
    "message": "Left under autorisering av bruker",
    "path": "/hentinntektliste"
}""".trimIndent()
