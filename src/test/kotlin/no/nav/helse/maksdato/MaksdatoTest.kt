package no.nav.helse.maksdato

import com.github.tomakehurst.wiremock.*
import com.github.tomakehurst.wiremock.client.*
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.*
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.*
import com.github.tomakehurst.wiremock.stubbing.Scenario.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.*

class MaksdatoTest {

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

    @BeforeEach
    fun configure() {
        WireMock.configureFor(MaksdatoTest.server.port())
    }

    @Test
    fun `maksdato service returns ok`() {
        stubFor(maksdatoMapping
                .willReturn(ok("2019-05-01"))
                .inScenario("default")
                .whenScenarioStateIs(STARTED))

        val requestBody = MaksdatoRequest(
                LocalDate.parse("2019-02-18"),
                LocalDate.parse("2019-02-18"),
                25,
                "ARBEIDSTAKER",
                emptyList())
        val maksdatoResponse = makeMaksdatoRequest("${server.baseUrl()}/maksdato", requestBody)
        assertEquals("2019-05-01", String(maksdatoResponse.data))
    }

    @Test
    fun `maksdato service returns error`() {
        stubFor(maksdatoMapping
                .willReturn(responseDefinition().withStatus(500))
                .inScenario("default")
                .whenScenarioStateIs(STARTED))

        val requestBody = MaksdatoRequest(
                LocalDate.parse("2019-02-18"),
                LocalDate.parse("2019-02-18"),
                25,
                "ARBEIDSTAKER",
                emptyList())
        val maksdatoResponse = makeMaksdatoRequest("${server.baseUrl()}/maksdato", requestBody)
        assertEquals(500, maksdatoResponse.statusCode)
    }


    private val maksdatoMapping: MappingBuilder = WireMock.post(WireMock.urlPathEqualTo("/maksdato"))
}