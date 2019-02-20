package no.nav.helse.ws.organisasjon

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.helse.assertJsonEquals
import no.nav.helse.bootstrapComponentTest
import no.nav.helse.mockedSparkel
import no.nav.helse.sts.StsRestClient
import no.nav.helse.ws.WsClients
import no.nav.helse.ws.sts.stsClient
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals

class OrganisasjonComponentTest {

    private val log = LoggerFactory.getLogger("OrganisasjonComponentTest")

    companion object {
        private val bootstrap = bootstrapComponentTest()
        private val token = bootstrap.jwkStub.createTokenFor("srvspinne")

        @BeforeAll
        @JvmStatic
        fun start() {
            bootstrap.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            bootstrap.stop()
        }
    }

    @BeforeEach
    @AfterEach
    fun `reset`() {
        bootstrap.reset()
    }

    @Test
    fun `Henting av organisasjon med en navnelinje`() {
        val orgNr = "971524960"

        val responses = OrganisasjonMocks.okResponses(
                orgNr = orgNr,
                navnLinje1 = "STORTINGET",
                forventetReturNavn = "STORTINGET"
        )
        OrganisasjonMocks.mock(
                orgNr = orgNr,
                xml = responses.registerXmlResponse
        )

        requestSparkelAndAssertResponse(
                orgNr = orgNr,
                expectedResponse = responses.sparkelJsonResponse
        )
    }

    @Test
    fun `Henting av organisasjon med tre navnelinjer`() {
        val orgNr = "971524961"

        val responses = OrganisasjonMocks.okResponses(
                orgNr = orgNr,
                navnLinje1 = "Baker Hansen",
                navnLinje2 = "Avdeling Sagene",
                navnLinje5 = "Gode kaker",
                forventetReturNavn = "Baker Hansen, Avdeling Sagene, Gode kaker"
        )
        OrganisasjonMocks.mock(
                orgNr = orgNr,
                xml = responses.registerXmlResponse
        )

        requestSparkelAndAssertResponse(
                orgNr = orgNr,
                expectedResponse = responses.sparkelJsonResponse
        )
    }

    @Test
    fun `Henting av organisasjon uten navnelinjer`() {
        val orgNr = "971524962"

        val responses = OrganisasjonMocks.okResponses(
                orgNr = orgNr,
                forventetReturNavn = null
        )
        OrganisasjonMocks.mock(
                orgNr = orgNr,
                xml = responses.registerXmlResponse
        )

        requestSparkelAndAssertResponse(
                orgNr = orgNr,
                expectedResponse = responses.sparkelJsonResponse
        )
    }

    @Test
    fun `Henting av kun organisasjons-navn`() {
        val orgNr = "971524963"

        val responses = OrganisasjonMocks.okResponses(
                orgNr = orgNr,
                navnLinje1 = "STORTINGET",
                forventetReturNavn = "STORTINGET"
        )
        OrganisasjonMocks.mock(
                orgNr = orgNr,
                xml = responses.registerXmlResponse
        )

        requestSparkelAndAssertResponse(
                orgNr = orgNr,
                expectedResponse = responses.sparkelJsonResponse,
                attributter = listOf("navn")
        )
    }

    @Test
    fun `Henting av ikke supportert attributt`() {
        val orgNr = "971524964"

        val expectedSparkelJsonResponse = OrganisasjonMocks.sparkelNotImplementedResponse()

        requestSparkelAndAssertResponse(
                orgNr = orgNr,
                expectedResponse = expectedSparkelJsonResponse,
                expectedHttpCode = 501,
                attributter = listOf("navn","addresse","telefonnummer")
        )
    }

    private fun requestSparkelAndAssertResponse(
            orgNr: String,
            attributter: List<String> = listOf(),
            expectedResponse : String,
            expectedHttpCode : Int = 200
    ) {
        var queryString= "?"
        attributter.forEach { it ->
            queryString = queryString.plus("attributt=$it&")
        }
        queryString = queryString.removeSuffix("?").removeSuffix("&")

        val url = "/api/organisasjon/$orgNr$queryString"
        log.info("URL=$url")

        val stsClientWs = stsClient(bootstrap.env.securityTokenServiceEndpointUrl,
                bootstrap.env.securityTokenUsername to bootstrap.env.securityTokenPassword)
        val stsClientRest = StsRestClient(
                bootstrap.env.stsRestUrl, bootstrap.env.securityTokenUsername, bootstrap.env.securityTokenPassword)

        val wsClients = WsClients(stsClientWs, stsClientRest, bootstrap.env.allowInsecureSoapRequests)

        withTestApplication({mockedSparkel(
                jwtIssuer = bootstrap.env.jwtIssuer,
                jwkProvider = bootstrap.jwkStub.stubbedJwkProvider(),
                organisasjonService = OrganisasjonService(wsClients.organisasjon(bootstrap.env.organisasjonEndpointUrl))
        )}) {
            handleRequest(HttpMethod.Get, url) {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(expectedHttpCode, response.status()?.value)
                assertJsonEquals(JSONObject(expectedResponse), JSONObject(response.content))
            }
        }
    }
}
