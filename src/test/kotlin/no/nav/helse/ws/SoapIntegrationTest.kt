package no.nav.helse.ws

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.prometheus.client.CollectorRegistry
import no.nav.helse.OppslagResult
import no.nav.helse.ws.person.Kjønn
import no.nav.helse.ws.person.Person
import no.nav.helse.ws.person.PersonClient
import no.nav.helse.ws.person.hentPersonStub
import no.nav.helse.ws.sts.STS_SAML_POLICY_NO_TRANSPORT_BINDING
import no.nav.helse.ws.sts.configureFor
import no.nav.helse.ws.sts.stsClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate

class SoapIntegrationTest {

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
    fun `sts should be called before making soap call`() {
        val stsClient = stsClient(server.baseUrl().plus("/sts"),
                "stsUsername" to "stsPassword"
        )

        val port = SoapPorts.PersonV3(server.baseUrl().plus("/person"))
        port.apply{stsClient.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)}
        val personClient = PersonClient(port)

        WireMock.stubFor(stsStub("stsUsername", "stsPassword")
                .willReturn(samlAssertionResponse("username", "issuer", "CN=B27 Issuing CA Intern, DC=preprod, DC=local", "digest",
                        "signature", "certificate"))
                .inScenario("default")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("token acquired"))

        WireMock.stubFor(hentPersonStub("1234567891011")
                .withSamlAssertion("username", "issuer", "CN=B27 Issuing CA Intern, DC=preprod, DC=local",
                        "digest", "signature", "certificate")
                .willReturn(WireMock.ok(hentPerson_response))
                .inScenario("default")
                .whenScenarioStateIs("token acquired")
                .willSetStateTo("personInfo called"))

        val actual = personClient.personInfo(AktørId("1234567891011"))
        val expected = Person(
                id = AktørId("1234567891011"),
                fornavn = "JENNY",
                mellomnavn = "PIKENES",
                etternavn = "LOLNES",
                fdato = LocalDate.of(1984, 7, 8),
                kjønn = Kjønn.KVINNE,
                bostedsland = "NOR"
        )
        when (actual) {
            is OppslagResult.Ok -> {
                Assertions.assertEquals(expected, actual.data)
            }
            is OppslagResult.Feil -> fail { "This lookup was expected to succeed, but it didn't" }
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
               <bostedsadresse>
                  <strukturertAdresse xsi:type="ns3:Gateadresse">
                     <landkode>NOR</landkode>
                     <tilleggsadresseType>Offisiell adresse</tilleggsadresseType>
                     <poststed>0557</poststed>
                     <kommunenummer>0301</kommunenummer>
                     <gatenavn>SANNERGATA</gatenavn>
                     <husnummer>2</husnummer>
                  </strukturertAdresse>
               </bostedsadresse>
               <sivilstand fomGyldighetsperiode="2018-03-01T00:00:00.000+01:00">
                  <sivilstand>UGIF</sivilstand>
               </sivilstand>
               <statsborgerskap>
                  <land>SWE</land>
               </statsborgerskap>
               <aktoer xsi:type="ns3:AktoerId">
                  <aktoerId>1234567891011</aktoerId>
               </aktoer>
               <kjoenn>
                  <kjoenn>K</kjoenn>
               </kjoenn>
               <personnavn>
                  <etternavn>LOLNES</etternavn>
                  <fornavn>JENNY</fornavn>
                  <mellomnavn>PIKENES</mellomnavn>
                  <sammensattNavn>LOLNES JENNY PIKENES</sammensattNavn>
               </personnavn>
               <personstatus>
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
