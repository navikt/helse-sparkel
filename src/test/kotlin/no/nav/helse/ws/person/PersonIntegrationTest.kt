package no.nav.helse.ws.person

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.like
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.ktor.http.HttpStatusCode
import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.sts.StsRestClient
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.WsClients
import no.nav.helse.ws.samlAssertionResponse
import no.nav.helse.ws.sts.stsClient
import no.nav.helse.ws.stsStub
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate

class PersonIntegrationTest {

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
        val client = WireMock.create().port(server.port()).build()
        WireMock.configureFor(client)
        client.resetMappings()
    }

    @Test
    fun `skal svare med gyldig person`() {
        val aktørId = "1234567891011"

        personStub(
                server = server,
                scenario = "person_hent_gyldig_person",
                request = hentPersonStub(aktørId),
                response = WireMock.ok(hentPerson_response)
        ) { personClient ->
            val expected = Person(AktørId(aktørId), "JENNY", "PIKENES", "LOLNES", LocalDate.of(1984, 7, 8), Kjønn.KVINNE, "NOR")
            val actual = personClient.personInfo(AktørId(aktørId))

            when (actual) {
                is OppslagResult.Ok -> {
                    assertEquals(expected, actual.data)
                }
                is OppslagResult.Feil -> fail { "Expected OppslagResult.Ok to be returned" }
            }
        }
    }

    @Test
    fun `skal svare med feil dersom personen ikke finnes`() {
        val aktørId = "1234567891011"

        personStub(
                server = server,
                scenario = "person_hent_ugyldig_person",
                request = hentPersonStub(aktørId),
                response = WireMock.serverError().withBody(hentPerson_not_found_response)
        ) { personClient ->
            val actual = personClient.personInfo(AktørId(aktørId))

            when (actual) {
                is OppslagResult.Feil -> {
                    when (actual.feil) {
                        is Feil.Exception -> {
                            assertEquals(HttpStatusCode.InternalServerError, actual.httpCode)
                            assertEquals("Ingen forekomster funnet", (actual.feil as Feil.Exception).feilmelding)
                            assertEquals("no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet", (actual.feil as Feil.Exception).exception.javaClass.name)
                        }
                        else -> fail { "Expected Feil.Exception to be returned" }
                    }
                }
                is OppslagResult.Ok -> fail { "Expected OppslagResult.Feil to be returned" }
            }
        }
    }
}

fun personStub(server: WireMockServer, scenario: String, response: ResponseDefinitionBuilder, request: MappingBuilder, test: (PersonClient) -> Unit) {
    val stsUsername = "stsUsername"
    val stsPassword = "stsPassword"

    val tokenSubject = "srvtestapp"
    val tokenIssuer = "Certificate Authority Inc"
    val tokenIssuerName = "CN=Certificate Authority Inc, DC=example, DC=com"
    val tokenDigest = "a random string"
    val tokenSignature = "yet another random string"
    val tokenCertificate = "one last random string"

    WireMock.stubFor(stsStub(stsUsername, stsPassword)
            .willReturn(samlAssertionResponse(tokenSubject, tokenIssuer, tokenIssuerName,
                    tokenDigest, tokenSignature, tokenCertificate))
            .inScenario(scenario)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("security_token_service_called"))

    WireMock.stubFor(request
            .withSamlAssertion(tokenSubject, tokenIssuer, tokenIssuerName,
                    tokenDigest, tokenSignature, tokenCertificate)
            .withCallId("Sett inn call id her")
            .willReturn(response)
            .inScenario(scenario)
            .whenScenarioStateIs("security_token_service_called")
            .willSetStateTo("person_stub_called"))

    val stsClientWs = stsClient(server.baseUrl().plus("/sts"), stsUsername to stsPassword)
    val stsClientRest = StsRestClient(server.baseUrl().plus("/sts"), stsUsername, stsPassword)

    val wsClients = WsClients(stsClientWs, stsClientRest, true)

    test(wsClients.person(server.baseUrl().plus("/person")))

    WireMock.listAllStubMappings().mappings.forEach {
        WireMock.verify(like(it.request))
    }
}

private val hentPerson_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
    <soapenv:Body>
        <ns2:hentPersonResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
            <response>
                <person xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns3:Bruker">
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
                    <sivilstand fomGyldighetsperiode="2019-01-21T00:00:00.000+01:00">
                        <sivilstand>GIFT</sivilstand>
                    </sivilstand>
                    <statsborgerskap>
                        <land>NOR</land>
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

private val hentPerson_not_found_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
    <soapenv:Body>
        <soapenv:Fault>
            <faultcode>soapenv:Server</faultcode>
            <faultstring>Ingen forekomster funnet</faultstring>
            <detail>
                <ns2:hentPersonpersonIkkeFunnet xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
                    <feilkilde>TPSWS</feilkilde>
                    <feilaarsak>Person med id 1234567891011 ikke funnet.</feilaarsak>
                    <feilmelding>Person ikke funnet</feilmelding>
                    <tidspunkt>2019-02-19T14:33:02.910+01:00</tidspunkt>
                </ns2:hentPersonpersonIkkeFunnet>
            </detail>
        </soapenv:Fault>
    </soapenv:Body>
</soapenv:Envelope>
""".trimIndent()

private const val hentPersonhistorikk_response = """
    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Body>
      <ns2:hentPersonhistorikkResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
         <response>
            <aktoer xsi:type="ns3:AktoerId" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
               <aktoerId>1234567891011</aktoerId>
            </aktoer>
            <personstatusListe endringstidspunkt="2019-01-21T00:00:00.000+01:00" endretAv="SKD" endringstype="endret">
               <periode>
                  <fom>1920-09-01T00:00:00.000+01:00</fom>
               </periode>
               <personstatus>BOSA</personstatus>
            </personstatusListe>
            <statsborgerskapListe endringstidspunkt="2019-01-21T00:00:00.000+01:00" endretAv="AJOURHD, SKD" endringstype="endret">
               <periode>
                  <fom>1920-09-01T00:00:00.000+01:00</fom>
               </periode>
               <statsborgerskap>
                  <land>NOR</land>
               </statsborgerskap>
            </statsborgerskapListe>
            <bostedsadressePeriodeListe endringstidspunkt="2019-01-21T00:00:00.000+01:00" endretAv="SKD" endringstype="endret">
               <periode>
                  <fom>1920-09-01T00:00:00.000+01:00</fom>
               </periode>
               <bostedsadresse>
                  <strukturertAdresse xsi:type="ns3:Gateadresse" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                     <landkode>NOR</landkode>
                     <poststed>0557</poststed>
                     <kommunenummer>0301</kommunenummer>
                     <gatenummer>16188</gatenummer>
                     <gatenavn>SANNERGATA</gatenavn>
                     <husnummer>2</husnummer>
                  </strukturertAdresse>
               </bostedsadresse>
            </bostedsadressePeriodeListe>
         </response>
      </ns2:hentPersonhistorikkResponse>
   </soapenv:Body>
</soapenv:Envelope>
"""
