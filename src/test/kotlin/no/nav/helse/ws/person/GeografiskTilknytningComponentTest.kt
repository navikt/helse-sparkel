package no.nav.helse.ws.person

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.helse.assertJsonEquals
import no.nav.helse.bootstrapComponentTest
import no.nav.helse.sparkel
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import no.nav.helse.ws.withSoapAction
import org.json.JSONObject
import org.junit.jupiter.api.*
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
        val responses = medDiskresjonsKodeResponses(aktoerId = aktoerId, kode = 6)

        WireMock.stubFor(medAktoerIdRequest(aktoerId)
                .withSamlAssertion()
                .withCallId()
                .willReturn(WireMock.okXml(responses.registerXmlResponse)))

        requestSparkelAndAssertResponse(aktoerId = aktoerId, expectedResponse = responses.sparkelJsonResponse)
    }

    @Test
    fun `Geografisk Tilknytning på en person med kode 7`() {
        val aktoerId = "1831212532189"
        val responses = medDiskresjonsKodeResponses(aktoerId = aktoerId, kode = 7)

        WireMock.stubFor(medAktoerIdRequest(aktoerId)
                .withSamlAssertion()
                .withCallId()
                .willReturn(WireMock.okXml(responses.registerXmlResponse)))

        requestSparkelAndAssertResponse(aktoerId = aktoerId, expectedResponse = responses.sparkelJsonResponse)
    }

    @Test
    fun `Geografisk Tilknytning på en person registrert på land`() {
        val aktoerId = "1831212532190"
        val geografiskOmraadeType = "Land"
        val geografiskOmraadeKode = "030103"

        val responses = medGeografiskTilknytningResponses(aktoerId = aktoerId, geografiskOmraadeType = geografiskOmraadeType, geografiskOmraadeKode = geografiskOmraadeKode)

        WireMock.stubFor(medAktoerIdRequest(aktoerId)
                .withSamlAssertion()
                .withCallId()
                .willReturn(WireMock.okXml(responses.registerXmlResponse)))

        requestSparkelAndAssertResponse(aktoerId = aktoerId, expectedResponse = responses.sparkelJsonResponse)
    }

    @Test
    fun `Geografisk Tilknytning på en person registrert på kommune`() {
        val aktoerId = "1831212532191"
        val geografiskOmraadeType = "Kommune"
        val geografiskOmraadeKode = "030104"

        val responses = medGeografiskTilknytningResponses(aktoerId = aktoerId, geografiskOmraadeType = geografiskOmraadeType, geografiskOmraadeKode = geografiskOmraadeKode)

        WireMock.stubFor(medAktoerIdRequest(aktoerId)
                .withSamlAssertion()
                .withCallId()
                .willReturn(WireMock.okXml(responses.registerXmlResponse)))

        requestSparkelAndAssertResponse(aktoerId = aktoerId, expectedResponse = responses.sparkelJsonResponse)

    }

    @Test
    fun `Geografisk Tilknytning på en person registrert på bydel`() {
        val aktoerId = "1831212532192"
        val geografiskOmraadeType = "Bydel"
        val geografiskOmraadeKode = "030105"

        val responses = medGeografiskTilknytningResponses(aktoerId = aktoerId, geografiskOmraadeType = geografiskOmraadeType, geografiskOmraadeKode = geografiskOmraadeKode)

        WireMock.stubFor(medAktoerIdRequest(aktoerId)
                .withSamlAssertion()
                .withCallId()
                .willReturn(WireMock.okXml(responses.registerXmlResponse)))

        requestSparkelAndAssertResponse(aktoerId = aktoerId, expectedResponse = responses.sparkelJsonResponse)
    }


    private fun requestSparkelAndAssertResponse(
            aktoerId: String,
            expectedResponse : String
    ) {
        withTestApplication({sparkel(bootstrap.env, bootstrap.jwkStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, "/api/person/$aktoerId/geografisk-tilknytning") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(200, response.status()?.value)
                assertJsonEquals(JSONObject(expectedResponse), JSONObject(response.content))
            }
        }
    }

    private fun medAktoerIdRequest(aktoerId: String): MappingBuilder {
        return WireMock.post(WireMock.urlPathEqualTo("/person"))
                .withSoapAction("http://nav.no/tjeneste/virksomhet/person/v3/Person_v3/hentGeografiskTilknytningRequest")
                .withRequestBody(ContainsPattern("<aktoerId>$aktoerId</aktoerId>"))
    }

    private fun medDiskresjonsKodeResponses(
            aktoerId: String,
            kode : Int
    ) : Responses {
        val diskresjonskode = if (kode == 6) "SPSF" else "SPFO"
        val beskrivelse = if (kode == 6) "Sperret adresse, strengt fortrolig" else "Sperret adresse, fortrolig"

        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Body>
                    <ns2:hentGeografiskTilknytningResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
                        <response>
                            <aktoer xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns3:AktoerId">
                                <aktoerId>$aktoerId</aktoerId>
                            </aktoer>
                            <navn>
                                <etternavn>DAME</etternavn>
                                <fornavn>SKJERMET</fornavn>
                                <sammensattNavn>DAME SKJERMET</sammensattNavn>
                            </navn>
                            <diskresjonskode>$diskresjonskode</diskresjonskode>
                        </response>
                    </ns2:hentGeografiskTilknytningResponse>
                </soapenv:Body>
            </soapenv:Envelope>
        """.trimIndent()

        val json = """
        {
            "diskresjonskode": {
                "forkortelse": "$diskresjonskode",
                "beskrivelse": "$beskrivelse",
                "kode": $kode
            }
        }
        """.trimIndent()

        return Responses(registerXmlResponse = xml, sparkelJsonResponse = json)
    }


    private fun medGeografiskTilknytningResponses(
            aktoerId: String,
            geografiskOmraadeType : String,
            geografiskOmraadeKode : String
    ) : Responses {
        val xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
            <soapenv:Body>
                <ns2:hentGeografiskTilknytningResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
                    <response>
                        <aktoer xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns3:AktoerId">
                            <aktoerId>$aktoerId</aktoerId>
                        </aktoer>
                        <navn>
                            <etternavn>LOLNES</etternavn>
                            <fornavn>JENNY</fornavn>
                            <mellomnavn>PIKENES</mellomnavn>
                            <sammensattNavn>LOLNES JENNY PIKENES</sammensattNavn>
                        </navn>
                        <geografiskTilknytning xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns3:$geografiskOmraadeType">
                            <geografiskTilknytning>$geografiskOmraadeKode</geografiskTilknytning>
                        </geografiskTilknytning>
                    </response>
                </ns2:hentGeografiskTilknytningResponse>
            </soapenv:Body>
        </soapenv:Envelope>
        """.trimIndent()

        val json = """
        {
            "geografiskOmraade": {
                "type": "${geografiskOmraadeType.toUpperCase()}",
                "kode": "$geografiskOmraadeKode"
            }
        }
        """.trimIndent()

        return Responses(registerXmlResponse = xml, sparkelJsonResponse = json)
    }

    private data class Responses(val registerXmlResponse : String, val sparkelJsonResponse : String)
}