package no.nav.helse.ws.person

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
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PersonComponentTest {

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
        WireMock.stubFor(hentPersonStub("1234567891011")
                .withSamlAssertion("testusername", "theIssuer", "CN=B27 Issuing CA Intern, DC=preprod, DC=local",
                        "digestValue", "signatureValue", "certificateValue")
                .withCallId("Sett inn call id her")
                .willReturn(WireMock.ok(hentPerson_response)))

        val env = Environment(mapOf(
                "SECURITY_TOKEN_SERVICE_URL" to server.baseUrl().plus("/sts"),
                "SECURITY_TOKEN_SERVICE_USERNAME" to "stsUsername",
                "SECURITY_TOKEN_SERVICE_PASSWORD" to "stsPassword",
                "PERSON_ENDPOINTURL" to server.baseUrl().plus("/person"),
                "JWT_ISSUER" to "test issuer",
                "ALLOW_INSECURE_SOAP_REQUESTS" to "true"
        ))

        withTestApplication({sparkel(env, jwtStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, "/api/person/1234567891011") {
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                Assertions.assertEquals(200, response.status()?.value)
                assertJsonEquals(JSONObject("{\"fdato\":\"1984-07-08\",\"etternavn\":\"LOLNES\",\"mellomnavn\":\"PIKENES\",\"id\":{\"aktor\":\"1234567891011\"},\"fornavn\":\"JENNY\",\"kjønn\":\"KVINNE\"}"), JSONObject(response.content))
            }
        }
    }
}

private val hentPerson_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Header xmlns:wsa="http://www.w3.org/2005/08/addressing">
      <wsa:Action>http://nav.no/tjeneste/virksomhet/person/v3/Person_v3/hentPersonResponse</wsa:Action>
   </soapenv:Header>
   <soapenv:Body>
      <ns2:hentPersonResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
         <response>
            <person xsi:type="ns3:Bruker" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
               <bostedsadresse endringstidspunkt="2018-11-07T00:00:00.000+01:00" endretAv="AJOURHD, SKD" endringstype="endret">
                  <strukturertAdresse xsi:type="ns3:Gateadresse">
                     <landkode>NOR</landkode>
                     <tilleggsadresseType>Offisiell adresse</tilleggsadresseType>
                     <poststed>0557</poststed>
                     <kommunenummer>0301</kommunenummer>
                     <gatenavn>SANNERGATA</gatenavn>
                     <husnummer>2</husnummer>
                  </strukturertAdresse>
               </bostedsadresse>
               <sivilstand endringstidspunkt="2018-11-07T00:00:00.000+01:00" endretAv="AJOURHD, SKD" endringstype="endret" fomGyldighetsperiode="2018-03-01T00:00:00.000+01:00">
                  <sivilstand>NULL</sivilstand>
               </sivilstand>
               <statsborgerskap endringstidspunkt="2018-11-07T00:00:00.000+01:00" endretAv="AJOURHD, SKD" endringstype="endret">
                  <land>NOR</land>
               </statsborgerskap>
               <aktoer xsi:type="ns3:AktoerId">
                  <aktoerId>1234567891011</aktoerId>
               </aktoer>
               <kjoenn>
                  <kjoenn>K</kjoenn>
               </kjoenn>
               <personnavn endringstidspunkt="2018-11-07T00:00:00.000+01:00" endretAv="AJOURHD, SKD" endringstype="endret">
                  <etternavn>LOLNES</etternavn>
                  <fornavn>JENNY</fornavn>
                  <mellomnavn>PIKENES</mellomnavn>
                  <sammensattNavn>LOLNES JENNY PIKENES</sammensattNavn>
               </personnavn>
               <personstatus endringstidspunkt="2018-11-07T00:00:00.000+01:00" endretAv="SKD" endringstype="endret">
                  <personstatus>BOSA</personstatus>
               </personstatus>
               <foedselsdato>
                  <foedselsdato>1984-07-08+02:00</foedselsdato>
               </foedselsdato>
               <gjeldendePostadressetype>BOSTEDSADRESSE</gjeldendePostadressetype>
               <geografiskTilknytning xsi:type="ns3:Bydel">
                  <geografiskTilknytning>030103</geografiskTilknytning>
               </geografiskTilknytning>
            </person>
         </response>
      </ns2:hentPersonResponse>
   </soapenv:Body>
</soapenv:Envelope>
""".trimIndent()
