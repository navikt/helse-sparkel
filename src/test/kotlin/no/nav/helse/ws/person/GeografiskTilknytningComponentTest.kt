package no.nav.helse.ws.person

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
import no.nav.helse.ws.person.GeografiskTilknytningMocks.medDiskresjonsKode6Responses
import no.nav.helse.ws.person.GeografiskTilknytningMocks.medDiskresjonsKode7Responses
import no.nav.helse.ws.person.GeografiskTilknytningMocks.medGeografiskTilknytningResponses
import no.nav.helse.ws.person.GeografiskTilknytningMocks.mock
import no.nav.helse.ws.person.GeografiskTilknytningMocks.utenGeografiskTilknytningEllerDiskresjonskode
import no.nav.helse.ws.sts.stsClient
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GeografiskTilknytningComponentTest {

    companion object {
        val bootstrap = bootstrapComponentTest()
        val token = bootstrap.jwkStub.createTokenFor("srvpleiepenger-opp")

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
    fun `Geografisk Tilknytning på en person med kode 6`() {
        val aktoerId = "1831212532188"
        val responses = medDiskresjonsKode6Responses(aktoerId = aktoerId)

        mock(aktoerId = aktoerId, xml = responses.registerXmlResponse)

        requestSparkelAndAssertResponse(
                aktoerId = aktoerId,
                expectedResponse = responses.sparkelJsonResponse,
                expectedHttpResponseCode = 403
        )
    }

    @Test
    fun `Geografisk Tilknytning på en person med kode 7`() {
        val aktoerId = "1831212532189"
        val geografiskOmraadeType = "Land"
        val geografiskOmraadeKode = "030155"

        val responses = medDiskresjonsKode7Responses(
                aktoerId = aktoerId,
                geografiskOmraadeType = geografiskOmraadeType,
                geografiskOmraadeKode = geografiskOmraadeKode
        )

        mock(aktoerId = aktoerId, xml = responses.registerXmlResponse)

        requestSparkelAndAssertResponse(
                aktoerId = aktoerId,
                expectedResponse = responses.sparkelJsonResponse
        )
    }

    @Test
    fun `Geografisk Tilknytning på en person registrert på land`() {
        val aktoerId = "1831212532190"
        val geografiskOmraadeType = "Land"
        val geografiskOmraadeKode = "030103"

        val responses = medGeografiskTilknytningResponses(
                aktoerId = aktoerId,
                geografiskOmraadeType = geografiskOmraadeType,
                geografiskOmraadeKode = geografiskOmraadeKode
        )

        mock(
                aktoerId = aktoerId,
                xml = responses.registerXmlResponse
        )

        requestSparkelAndAssertResponse(
                aktoerId = aktoerId,
                expectedResponse = responses.sparkelJsonResponse
        )
    }

    @Test
    fun `Geografisk Tilknytning på en person registrert på kommune`() {
        val aktoerId = "1831212532191"
        val geografiskOmraadeType = "Kommune"
        val geografiskOmraadeKode = "030104"

        val responses = medGeografiskTilknytningResponses(
                aktoerId = aktoerId,
                geografiskOmraadeType = geografiskOmraadeType,
                geografiskOmraadeKode = geografiskOmraadeKode
        )

        mock(
                aktoerId = aktoerId,
                xml = responses.registerXmlResponse
        )

        requestSparkelAndAssertResponse(
                aktoerId = aktoerId,
                expectedResponse = responses.sparkelJsonResponse
        )

    }

    @Test
    fun `Geografisk Tilknytning på en person registrert på bydel`() {
        val aktoerId = "1831212532192"
        val geografiskOmraadeType = "Bydel"
        val geografiskOmraadeKode = "030105"

        val responses = medGeografiskTilknytningResponses(
                aktoerId = aktoerId,
                geografiskOmraadeType = geografiskOmraadeType,
                geografiskOmraadeKode = geografiskOmraadeKode
        )

        mock(
                aktoerId = aktoerId,
                xml = responses.registerXmlResponse
        )

        requestSparkelAndAssertResponse(
                aktoerId = aktoerId,
                expectedResponse = responses.sparkelJsonResponse
        )
    }

    @Test
    fun `Person uten Geografisk Tilnytning`() {
        val aktoerId = "1831212532193"

        val responses = utenGeografiskTilknytningEllerDiskresjonskode(
                aktoerId = aktoerId
        )

        mock(
                aktoerId = aktoerId,
                xml = responses.registerXmlResponse
        )

        requestSparkelAndAssertResponse(
                aktoerId = aktoerId,
                expectedResponse = responses.sparkelJsonResponse,
                expectedHttpResponseCode = 404
        )
    }


    private fun requestSparkelAndAssertResponse(
            aktoerId: String,
            expectedResponse : String,
            expectedHttpResponseCode : Int = 200
    ) {
        val stsClientWs = stsClient(bootstrap.env.securityTokenServiceEndpointUrl,
                bootstrap.env.securityTokenUsername to bootstrap.env.securityTokenPassword)
        val stsClientRest = StsRestClient(
                bootstrap.env.stsRestUrl, bootstrap.env.securityTokenUsername, bootstrap.env.securityTokenPassword)

        val wsClients = WsClients(stsClientWs, stsClientRest, bootstrap.env.allowInsecureSoapRequests)

        withTestApplication({mockedSparkel(
                jwtIssuer = bootstrap.env.jwtIssuer,
                jwkProvider = bootstrap.jwkStub.stubbedJwkProvider(),
                personService = PersonService(wsClients.person(bootstrap.env.personEndpointUrl))
        )}) {
            handleRequest(HttpMethod.Get, "/api/person/$aktoerId/geografisk-tilknytning") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(expectedHttpResponseCode, response.status()?.value)
                assertJsonEquals(JSONObject(expectedResponse), JSONObject(response.content))
            }
        }
    }
}
