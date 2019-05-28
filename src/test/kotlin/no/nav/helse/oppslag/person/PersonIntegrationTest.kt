package no.nav.helse.oppslag.person

import arrow.core.Try
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.like
import com.github.tomakehurst.wiremock.stubbing.Scenario
import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.AktørId
import no.nav.helse.sts.StsRestClient
import no.nav.helse.oppslag.*
import no.nav.helse.oppslag.sts.stsClient
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import kotlin.test.assertNull

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
    fun `skal svare med gyldig person med adresse`() {
        val aktørId = "1234567891011"

        personStub(
                server = server,
                scenario = "person_hent_gyldig_person_med_adresse",
                request = hentPersonStub(aktørId),
                response = WireMock.ok(hentPersonMedAdresse_response)
        ) { personClient ->
            val actual = personClient.person(AktørId(aktørId))

            when (actual) {
                is Try.Success -> {
                    assertTrue(actual.value.aktoer is AktoerId)
                    assertEquals(aktørId, (actual.value.aktoer as AktoerId).aktoerId)
                    assertEquals("JENNY", actual.value.personnavn.fornavn)
                    assertEquals("PIKENES", actual.value.personnavn.mellomnavn)
                    assertEquals("LOLNES", actual.value.personnavn.etternavn)
                    assertEquals(LocalDate.of(1984, 7, 8), actual.value.foedselsdato.foedselsdato.toLocalDate())
                    assertEquals("K", actual.value.kjoenn.kjoenn.value)
                    assertEquals("NOR", actual.value.bostedsadresse.strukturertAdresse.landkode.value)
                }
                is Try.Failure -> fail { "Expected Try.Success to be returned" }
            }
        }
    }

    @Test
    fun `skal svare med gyldig person uten adresse`() {
        val aktørId = "1234567812345"

        personStub(
                server = server,
                scenario = "person_hent_gyldig_person_uten_adresse",
                request = hentPersonStub(aktørId),
                response = WireMock.ok(hentPersonUtenAdresse_response)
        ) { personClient ->
            val actual = personClient.personMedAdresse(AktørId(aktørId))

            when (actual) {
                is Try.Success -> {
                    assertTrue(actual.value.aktoer is AktoerId)
                    assertEquals(aktørId, (actual.value.aktoer as AktoerId).aktoerId)
                    assertEquals("OLA", actual.value.personnavn.fornavn)
                    assertEquals("ANDREAS", actual.value.personnavn.mellomnavn)
                    assertEquals("ETTERNAVN", actual.value.personnavn.etternavn)
                    assertEquals(LocalDate.of(1970, 8, 23), actual.value.foedselsdato.foedselsdato.toLocalDate())
                    assertEquals("K", actual.value.kjoenn.kjoenn.value)
                    assertNull(actual.value.bostedsadresse)
                }
                is Try.Failure -> fail { "Expected Try.Success to be returned" }
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
            val actual = personClient.personMedAdresse(AktørId(aktørId))

            when (actual) {
                is Try.Failure -> {
                    when (actual.exception) {
                        is HentPersonPersonIkkeFunnet -> assertEquals("Ingen forekomster funnet", actual.exception.message)
                        else -> fail { "Expected HentPersonPersonIkkeFunnet to be returned" }
                    }
                }
                is Try.Success -> fail { "Expected Try.Failure to be returned" }
            }
        }
    }

    @Test
    fun `oppslag på familierelasjoner`() {
        val aktørId = "1000000000000"

        personStub(
                server = server,
                scenario = "person_hent_familierelasjoner",
                request = hentPersonStub(aktørId),
                response = WireMock.ok(hentPerson_med_familierelasjoner)
        ) { personClient ->
            val actual = personClient.familierelasjoner(AktørId(aktørId))

            when (actual) {
                is Try.Success -> {
                    assertEquals(4, actual.value.size)
                    assertTrue(actual.value.inneholderAktør(aktørId = "1000000000004", fornavn = "MOR", etternavn = "MORSEN", relasjon = "MORA"))
                    assertTrue(actual.value.inneholderAktør(aktørId = "1000000000003", fornavn = "FAR", mellomnavn = "FAR", etternavn = "FARSEN", relasjon = "FARA"))
                    assertTrue(actual.value.inneholderAktør(aktørId = "1000000000001", fornavn = "BARN", mellomnavn = "EN", etternavn = "BARNSEN", relasjon = "BARN"))
                    assertTrue(actual.value.inneholderAktør(aktørId = "1000000000002", fornavn = "BARN", mellomnavn = "TO", etternavn = "BARNSEN", relasjon = "BARN"))
                }
                is Try.Failure -> fail { "Expected Try.Success to be returned" }
            }
        }
    }
}

private fun List<Familierelasjon>.inneholderAktør(
        aktørId: String,
        relasjon: String,
        fornavn: String,
        mellomnavn: String? = null,
        etternavn: String
) = filter { it.tilRolle.value == relasjon }
        .filter { (it.tilPerson.aktoer as AktoerId).aktoerId == aktørId }
        .filter { it.tilPerson.personnavn.fornavn == fornavn }
        .filter { it.tilPerson.personnavn.mellomnavn == mellomnavn }
        .filter { it.tilPerson.personnavn.etternavn == etternavn }
        .size == 1

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
            .withCallId()
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

private val hentPersonMedAdresse_response = """
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

private val hentPersonUtenAdresse_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
    <soapenv:Body>
        <ns2:hentPersonResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
            <response>
                <person xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns3:Bruker">
                    <sivilstand fomGyldighetsperiode="1989-11-10T00:00:00.000+01:00">
                        <sivilstand>SKIL</sivilstand>
                    </sivilstand>
                    <statsborgerskap>
                        <land>NOR</land>
                    </statsborgerskap>
                    <aktoer xsi:type="ns3:AktoerId">
                        <aktoerId>1234567812345</aktoerId>
                    </aktoer>
                    <kjoenn>
                        <kjoenn>K</kjoenn>
                    </kjoenn>
                    <personnavn>
                        <etternavn>ETTERNAVN</etternavn>
                        <fornavn>OLA</fornavn>
                        <mellomnavn>ANDREAS</mellomnavn>
                        <sammensattNavn>OLA ANDREAS ETTERNAVN</sammensattNavn>
                    </personnavn>
                    <personstatus>
                        <personstatus>BOSA</personstatus>
                    </personstatus>
                    <foedselsdato>
                        <foedselsdato>1970-08-23+01:00</foedselsdato>
                    </foedselsdato>
                    <geografiskTilknytning xsi:type="ns3:Kommune">
                        <geografiskTilknytning>0219</geografiskTilknytning>
                    </geografiskTilknytning>
                </person>
            </response>
        </ns2:hentPersonResponse>
    </soapenv:Body>
</soapenv:Envelope>
""".trimIndent()

private val hentPerson_med_familierelasjoner = """
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
    <soapenv:Body>
        <ns2:hentPersonResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
            <response>
                <person xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns3:Bruker">
                    <sivilstand fomGyldighetsperiode="1989-11-10T00:00:00.000+01:00">
                        <sivilstand>SKIL</sivilstand>
                    </sivilstand>
                    <statsborgerskap>
                        <land>NOR</land>
                    </statsborgerskap>
                    <harFraRolleI>
                        <harSammeBosted>false</harSammeBosted>
                        <tilRolle>MORA</tilRolle>
                        <tilPerson>
                            <aktoer xsi:type="ns3:AktoerId">
                                <aktoerId>1000000000004</aktoerId>
                            </aktoer>
                            <personnavn>
                                <etternavn>MORSEN</etternavn>
                                <fornavn>MOR</fornavn>
                                <sammensattNavn>MOR MORSEN</sammensattNavn>
                            </personnavn>
                            <personstatus>
                                <personstatus>DØD</personstatus>
                            </personstatus>
                            <doedsdato>
                                <doedsdato>2005-03-08+01:00</doedsdato>
                            </doedsdato>
                        </tilPerson>
                    </harFraRolleI>
                    <harFraRolleI>
                        <harSammeBosted>false</harSammeBosted>
                        <tilRolle>FARA</tilRolle>
                        <tilPerson>
                            <aktoer xsi:type="ns3:AktoerId">
                                <aktoerId>1000000000003</aktoerId>
                            </aktoer>
                            <personnavn>
                                <etternavn>FARSEN</etternavn>
                                <fornavn>FAR</fornavn>
                                <mellomnavn>FAR</mellomnavn>
                                <sammensattNavn>FAR FAR FARSEN</sammensattNavn>
                            </personnavn>
                            <personstatus>
                                <personstatus>DØD</personstatus>
                            </personstatus>
                            <doedsdato>
                                <doedsdato>1992-10-23+01:00</doedsdato>
                            </doedsdato>
                        </tilPerson>
                    </harFraRolleI>
                    <harFraRolleI>
                        <harSammeBosted>false</harSammeBosted>
                        <tilRolle>BARN</tilRolle>
                        <tilPerson>
                            <aktoer xsi:type="ns3:AktoerId">
                                <aktoerId>1000000000001</aktoerId>
                            </aktoer>
                            <personnavn>
                                <etternavn>BARNSEN</etternavn>
                                <fornavn>BARN</fornavn>
                                <mellomnavn>EN</mellomnavn>
                                <sammensattNavn>BARN BARN BARNSEN</sammensattNavn>
                            </personnavn>
                        </tilPerson>
                    </harFraRolleI>
                    <harFraRolleI>
                        <harSammeBosted>false</harSammeBosted>
                        <tilRolle>BARN</tilRolle>
                        <tilPerson>
                            <aktoer xsi:type="ns3:AktoerId">
                                <aktoerId>1000000000002</aktoerId>
                            </aktoer>
                            <personnavn>
                                <etternavn>BARNSEN</etternavn>
                                <fornavn>BARN</fornavn>
                                <mellomnavn>TO</mellomnavn>
                                <sammensattNavn>BARN TO BARNSEN</sammensattNavn>
                            </personnavn>
                        </tilPerson>
                    </harFraRolleI>
                    <aktoer xsi:type="ns3:AktoerId">
                        <aktoerId>1000000000000</aktoerId>
                    </aktoer>
                    <kjoenn>
                        <kjoenn>K</kjoenn>
                    </kjoenn>
                    <personnavn>
                        <etternavn>KARISON</etternavn>
                        <fornavn>KARI</fornavn>
                        <mellomnavn>KARI</mellomnavn>
                        <sammensattNavn>KARI KARI KARISON</sammensattNavn>
                    </personnavn>
                    <personstatus>
                        <personstatus>BOSA</personstatus>
                    </personstatus>
                    <foedselsdato>
                        <foedselsdato>1956-08-09+01:00</foedselsdato>
                    </foedselsdato>
                    <geografiskTilknytning xsi:type="ns3:Kommune">
                        <geografiskTilknytning>0219</geografiskTilknytning>
                    </geografiskTilknytning>
                </person>
            </response>
        </ns2:hentPersonResponse>
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
