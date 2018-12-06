package no.nav.helse.ws.organisasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.prometheus.client.CollectorRegistry
import no.nav.helse.Environment
import no.nav.helse.JwtStub
import no.nav.helse.assertJsonEquals
import no.nav.helse.sparkel
import no.nav.helse.ws.samlAssertionResponse
import no.nav.helse.ws.stsStub
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import org.json.JSONObject
import org.junit.jupiter.api.*



class OrganisasjonComponentTest {

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
        WireMock.configureFor(server.port())
    }

    @AfterEach
    fun `clear prometheus registry`() {
        CollectorRegistry.defaultRegistry.clear()
    }

    @Test
    fun `that response is json`() {
        val jwtStub = JwtStub("test issuer")
        val token = jwtStub.createTokenFor("srvspinne")

        WireMock.stubFor(stsStub("stsUsername", "stsPassword")
                .willReturn(samlAssertionResponse("testusername", "theIssuer", "CN=B27 Issuing CA Intern, DC=preprod, DC=local",
                        "digestValue", "signatureValue", "certificateValue")))

        WireMock.stubFor(hentOrganisasjonStub("13119924167")
                .withSamlAssertion("testusername", "theIssuer", "CN=B27 Issuing CA Intern, DC=preprod, DC=local",
                        "digestValue", "signatureValue", "certificateValue")
                .withCallId("Sett inn call id her")
                .willReturn(WireMock.okForContentType("application/xml; charset=utf-8", hentOrganisasjon_response)))

        val env = Environment(mapOf(
                "SECURITY_TOKEN_SERVICE_URL" to server.baseUrl().plus("/sts"),
                "SECURITY_TOKEN_SERVICE_USERNAME" to "stsUsername",
                "SECURITY_TOKEN_SERVICE_PASSWORD" to "stsPassword",
                "ORGANISASJON_ENDPOINTURL" to server.baseUrl().plus("/organisasjon"),
                "JWT_ISSUER" to "test issuer",
                "ALLOW_INSECURE_SOAP_REQUESTS" to "true"
        ))

        withTestApplication({sparkel(env, jwtStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Post, "/api/organisasjon") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
                setBody("{\"orgnr\": \"13119924167\"}")
            }.apply {
                Assertions.assertEquals(200, response.status()?.value)
                assertJsonEquals(JSONObject(expectedJson), JSONObject(response.content))
            }
        }
    }
}

private val hentOrganisasjon_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:hentOrganisasjonResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/organisasjon/v5">
         <response>
            <organisasjon xsi:type="ns4:Virksomhet" xmlns:ns4="http://nav.no/tjeneste/virksomhet/organisasjon/v5/informasjon" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
               <orgnummer>910825186</orgnummer>
               <navn xsi:type="ns4:UstrukturertNavn">
                  <navnelinje>EGERSUND OG ØRSTA REGNSKAP</navnelinje>
                  <navnelinje/>
                  <navnelinje/>
                  <navnelinje/>
                  <navnelinje/>
               </navn>
               <organisasjonDetaljer>
                  <registreringsDato>2018-09-03+02:00</registreringsDato>
                  <datoSistEndret>2018-10-23+02:00</datoSistEndret>
                  <forretningsadresse fomBruksperiode="2018-10-24T13:12:34.739+02:00" fomGyldighetsperiode="2018-10-23T00:00:00.000+02:00" xsi:type="ns4:SemistrukturertAdresse">
                     <adresseId>Vei 123456</adresseId>
                     <landkode kodeRef="NO"/>
                     <adresseledd>
                        <noekkel kodeRef="adresselinje1"/>
                        <verdi>Stakkevollvegen 328</verdi>
                     </adresseledd>
                     <adresseledd>
                        <noekkel kodeRef="postnr"/>
                        <verdi>9019</verdi>
                     </adresseledd>
                     <adresseledd>
                        <noekkel kodeRef="kommunenr"/>
                        <verdi>1902</verdi>
                     </adresseledd>
                     <adresseledd>
                        <noekkel kodeRef="linjenummer"/>
                        <verdi>1</verdi>
                     </adresseledd>
                  </forretningsadresse>
                  <postadresse fomBruksperiode="2018-10-24T13:12:34.740+02:00" fomGyldighetsperiode="2018-10-23T00:00:00.000+02:00" xsi:type="ns4:SemistrukturertAdresse">
                     <adresseId>Vei 123456</adresseId>
                     <landkode kodeRef="NO"/>
                     <adresseledd>
                        <noekkel kodeRef="adresselinje1"/>
                        <verdi>Stakkevollvegen 328</verdi>
                     </adresseledd>
                     <adresseledd>
                        <noekkel kodeRef="postnr"/>
                        <verdi>9019</verdi>
                     </adresseledd>
                     <adresseledd>
                        <noekkel kodeRef="kommunenr"/>
                        <verdi>1902</verdi>
                     </adresseledd>
                     <adresseledd>
                        <noekkel kodeRef="linjenummer"/>
                        <verdi>1</verdi>
                     </adresseledd>
                  </postadresse>
                  <navSpesifikkInformasjon fomBruksperiode="1900-01-01T00:00:00.000+01:00" fomGyldighetsperiode="1900-01-01T00:00:00.000+01:00">
                     <erIA>false</erIA>
                  </navSpesifikkInformasjon>
                  <navn fomBruksperiode="2018-10-24T13:12:33.501+02:00" fomGyldighetsperiode="2018-10-23T00:00:00.000+02:00">
                     <navn xsi:type="ns4:UstrukturertNavn">
                        <navnelinje>EGERSUND OG ØRSTA REGNSKAP</navnelinje>
                        <navnelinje/>
                        <navnelinje/>
                        <navnelinje/>
                        <navnelinje/>
                     </navn>
                  </navn>
                  <organisasjonEnhetstyper fomBruksperiode="2018-09-04T14:51:15.327+02:00" fomGyldighetsperiode="2018-09-04T00:00:00.000+02:00">
                     <enhetstype>Bedrift (BEDR)</enhetstype>
                  </organisasjonEnhetstyper>
               </organisasjonDetaljer>
               <virksomhetDetaljer>
                  <enhetstype kodeRef="BEDR"/>
               </virksomhetDetaljer>
            </organisasjon>
         </response>
      </ns2:hentOrganisasjonResponse>
   </soap:Body>
</soap:Envelope>
""".trimIndent()

private val expectedJson = """
{
    "navn":"EGERSUND OG ØRSTA REGNSKAP"
}
""".trimIndent()
