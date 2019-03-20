package no.nav.helse.ws.organisasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.stubbing.Scenario
import no.nav.helse.Either
import no.nav.helse.sts.StsRestClient
import no.nav.helse.ws.WsClients
import no.nav.helse.ws.samlAssertionResponse
import no.nav.helse.ws.sts.stsClient
import no.nav.helse.ws.stsStub
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import no.nav.helse.ws.withSoapAction
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate
import kotlin.test.assertEquals

class OrganisasjonIntegrationTest {

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
    fun `skal svare med feil når tjenesten svarer med feil`() {
        val orgNr = "971524960"

        val requestStub = WireMock.post(WireMock.urlPathEqualTo("/organisasjon"))
                .withSoapAction("http://nav.no/tjeneste/virksomhet/organisasjon/v5/BindinghentOrganisasjon/")
                .withRequestBody(ContainsPattern("<orgnummer>$orgNr</orgnummer>"))

        organisasjonStub(
                server = server,
                scenario = "organisasjon_hent_navn",
                request = requestStub,
                response = WireMock.serverError().withBody(faultXml("SOAP fault"))
        ) { organisasjonClient ->
            val actual = organisasjonClient.hentOrganisasjon(OrganisasjonsNummer(orgNr))

            when (actual) {
                is Either.Left -> assertEquals("SOAP fault", actual.left.message)
                is Either.Right -> fail { "Expected Either.Left to be returned" }
            }
        }
    }

    @Test
    fun `Henting av organisasjon`() {
        val orgNr = "971524963"

        val requestStub = WireMock.post(WireMock.urlPathEqualTo("/organisasjon"))
                .withSoapAction("http://nav.no/tjeneste/virksomhet/organisasjon/v5/BindinghentOrganisasjon/")
                .withRequestBody(ContainsPattern("<orgnummer>$orgNr</orgnummer>"))

        organisasjonStub(
                server = server,
                scenario = "organisasjon_hent_organisasjon",
                request = requestStub,
                response = WireMock.okXml(okXml(
                        orgNr = orgNr,
                        navnLinje1 = "NAV",
                        navnLinje2 = "AVD SANNERGATA 2"
                ))
        ) { organisasjonClient ->
            val actual = organisasjonClient.hentOrganisasjon(OrganisasjonsNummer(orgNr))

            when (actual) {
                is Either.Right -> {
                    assertEquals(orgNr, actual.right.orgnummer)
                    assertEquals("NAV", (actual.right.navn as UstrukturertNavn).navnelinje[0])
                    assertEquals("AVD SANNERGATA 2", (actual.right.navn as UstrukturertNavn).navnelinje[1])
                }
                is Either.Left -> fail { "Expected Either.Right to be returned" }
            }
        }
    }

    @Test
    fun `Henting av organisasjon uten navnelinjer`() {
        val orgNr = "971524963"

        val requestStub = WireMock.post(WireMock.urlPathEqualTo("/organisasjon"))
                .withSoapAction("http://nav.no/tjeneste/virksomhet/organisasjon/v5/BindinghentOrganisasjon/")
                .withRequestBody(ContainsPattern("<orgnummer>$orgNr</orgnummer>"))

        organisasjonStub(
                server = server,
                scenario = "organisasjon_hent_organisasjon",
                request = requestStub,
                response = WireMock.okXml(okXml(
                        orgNr = orgNr
                ))
        ) { organisasjonClient ->
            val actual = organisasjonClient.hentOrganisasjon(OrganisasjonsNummer(orgNr))

            when (actual) {
                is Either.Right -> {
                    assertEquals(orgNr, actual.right.orgnummer)
                    assertEquals(5, (actual.right.navn as UstrukturertNavn).navnelinje.size)
                    assertNull((actual.right.navn as UstrukturertNavn).navnelinje.firstOrNull { it.isNotBlank() })
                }
                is Either.Left -> fail { "Expected Either.Right to be returned" }
            }
        }
    }

    @Test
    fun `skal svare med feil når virksomhetsoppslag gir feil`() {
        val juridiskOrgnr = "987654321"

        val requestStub = WireMock.post(WireMock.urlPathEqualTo("/organisasjon"))
                .withSoapAction("http://nav.no/tjeneste/virksomhet/organisasjon/v5/BindinghentVirksomhetsOrgnrForJuridiskOrgnrBolk/")
                .withRequestBody(ContainsPattern("<organisasjonsnummer>$juridiskOrgnr</organisasjonsnummer>"))
                .withRequestBody(ContainsPattern("<hentingsdato>2019-01-01Z</hentingsdato>"))

        organisasjonStub(
                server = server,
                scenario = "organisasjon_hent_virksomheter",
                request = requestStub,
                response = WireMock.serverError().withBody(faultXml("SOAP fault"))
        ) { organisasjonClient ->
            val actual = organisasjonClient.hentVirksomhetForJuridiskOrganisasjonsnummer(OrganisasjonsNummer(juridiskOrgnr),
                    LocalDate.parse("2019-01-01"))

            when (actual) {
                is Either.Left -> assertEquals("SOAP fault", actual.left.message)
                is Either.Right -> fail { "Expected Either.Left to be returned" }
            }
        }
    }

    @Test
    fun `skal svare med med unntaksliste`() {
        val juridiskOrgnr = "987654321"

        val requestStub = WireMock.post(WireMock.urlPathEqualTo("/organisasjon"))
                .withSoapAction("http://nav.no/tjeneste/virksomhet/organisasjon/v5/BindinghentVirksomhetsOrgnrForJuridiskOrgnrBolk/")
                .withRequestBody(ContainsPattern("<organisasjonsnummer>$juridiskOrgnr</organisasjonsnummer>"))
                .withRequestBody(ContainsPattern("<hentingsdato>2019-01-01Z</hentingsdato>"))

        organisasjonStub(
                server = server,
                scenario = "organisasjon_hent_virksomheter",
                request = requestStub,
                response = WireMock.okXml(virksomhetFinnesIkke(juridiskOrgnr))
                        .withHeader("Content-type", "application/xml;charset=utf-8")
        ) { organisasjonClient ->
            val actual = organisasjonClient.hentVirksomhetForJuridiskOrganisasjonsnummer(OrganisasjonsNummer(juridiskOrgnr),
                    LocalDate.parse("2019-01-01"))

            when (actual) {
                is Either.Right -> assertEquals("$juridiskOrgnr er opphørt eller eksisterer ikke på dato 2019-01-01", actual.right.unntakForOrgnrListe[0].unntaksmelding)
                is Either.Left -> fail { "Expected Either.Right to be returned" }
            }
        }
    }
}

fun organisasjonStub(server: WireMockServer, scenario: String, request: MappingBuilder, response: ResponseDefinitionBuilder, test: (OrganisasjonClient) -> Unit) {
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
            .willSetStateTo("organisasjon_stub_called"))

    val stsClientWs = stsClient(server.baseUrl().plus("/sts"), stsUsername to stsPassword)
    val stsClientRest = StsRestClient(server.baseUrl().plus("/sts"), stsUsername, stsPassword)

    val wsClients = WsClients(stsClientWs, stsClientRest, true)

    test(wsClients.organisasjon(server.baseUrl().plus("/organisasjon")))

    WireMock.listAllStubMappings().mappings.forEach {
        WireMock.verify(RequestPatternBuilder.like(it.request))
    }
}

private fun okXml(orgNr: String,
                  navnLinje1: String = "",
                  navnLinje2: String = "",
                  navnLinje3: String = "",
                  navnLinje4: String = "",
                  navnLinje5: String = "") = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body>
        <ns2:hentOrganisasjonResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/organisasjon/v5">
            <response>
                <organisasjon xmlns:ns4="http://nav.no/tjeneste/virksomhet/organisasjon/v5/informasjon" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns4:Virksomhet">
                    <orgnummer>$orgNr</orgnummer>
                    <navn xsi:type="ns4:UstrukturertNavn">
                        <navnelinje>$navnLinje1</navnelinje>
                        <navnelinje>$navnLinje2</navnelinje>
                        <navnelinje>$navnLinje3</navnelinje>
                        <navnelinje>$navnLinje4</navnelinje>
                        <navnelinje>$navnLinje5</navnelinje>
                    </navn>
                    <organisasjonDetaljer>
                        <registreringsDato>1995-08-09+02:00</registreringsDato>
                        <datoSistEndret>2018-06-06+02:00</datoSistEndret>
                        <gjeldendeMaalform kodeRef="NB"/>
                        <registrertMVA fomBruksperiode="2014-05-21T20:06:47+02:00" fomGyldighetsperiode="2014-03-18T00:00:00.000+01:00">
                            <registrertIMVA>true</registrertIMVA>
                        </registrertMVA>
                        <telefaks fomBruksperiode="2014-05-21T20:06:47+02:00" fomGyldighetsperiode="2014-03-18T00:00:00.000+01:00">
                            <identifikator>23 31 38 50</identifikator>
                        </telefaks>
                        <telefon fomBruksperiode="2014-05-21T20:06:47+02:00" fomGyldighetsperiode="2014-03-18T00:00:00.000+01:00">
                            <identifikator>23 31 30 50</identifikator>
                        </telefon>
                        <forretningsadresse fomBruksperiode="2015-02-23T10:38:34.403+01:00" fomGyldighetsperiode="2013-07-09T00:00:00.000+02:00" xsi:type="ns4:SemistrukturertAdresse">
                            <landkode kodeRef="NO"/>
                            <adresseledd>
                                <noekkel kodeRef="adresselinje1"/>
                                <verdi>Karl Johans gate 22</verdi>
                            </adresseledd>
                            <adresseledd>
                                <noekkel kodeRef="postnr"/>
                                <verdi>0026</verdi>
                            </adresseledd>
                            <adresseledd>
                                <noekkel kodeRef="kommunenr"/>
                                <verdi>0301</verdi>
                            </adresseledd>
                        </forretningsadresse>
                        <postadresse fomBruksperiode="2016-10-06T04:04:32.280+02:00" fomGyldighetsperiode="2016-10-05T00:00:00.000+02:00" xsi:type="ns4:SemistrukturertAdresse">
                            <landkode kodeRef="NO"/>
                            <adresseledd>
                                <noekkel kodeRef="adresselinje1"/>
                                <verdi>Postboks 1700 Sentrum</verdi>
                            </adresseledd>
                            <adresseledd>
                                <noekkel kodeRef="postnr"/>
                                <verdi>0026</verdi>
                            </adresseledd>
                            <adresseledd>
                                <noekkel kodeRef="kommunenr"/>
                                <verdi>0301</verdi>
                            </adresseledd>
                        </postadresse>
                        <navSpesifikkInformasjon fomBruksperiode="2014-11-28T08:10:38+01:00" fomGyldighetsperiode="2014-11-28T08:10:38+01:00">
                            <erIA>true</erIA>
                        </navSpesifikkInformasjon>
                        <internettadresse fomBruksperiode="2014-05-21T20:06:47+02:00" fomGyldighetsperiode="2014-03-18T00:00:00.000+01:00">
                            <identifikator>www.stortinget.no/</identifikator>
                        </internettadresse>
                        <epostadresse fomBruksperiode="2014-05-21T20:06:47+02:00" fomGyldighetsperiode="2014-03-18T00:00:00.000+01:00">
                            <identifikator>stortinget.postmottak@stortinget.no</identifikator>
                        </epostadresse>
                        <naering fomBruksperiode="2014-05-22T00:55:16+02:00" fomGyldighetsperiode="1965-12-31T00:00:00.000+01:00">
                            <naeringskode kodeRef="84.110"/>
                            <hjelpeenhet>false</hjelpeenhet>
                        </naering>
                        <navn fomBruksperiode="2015-02-23T08:04:53.200+01:00" fomGyldighetsperiode="1998-10-16T00:00:00.000+02:00">
                            <navn xsi:type="ns4:UstrukturertNavn">
                                <navnelinje>$navnLinje1</navnelinje>
                                <navnelinje>$navnLinje2</navnelinje>
                                <navnelinje>$navnLinje3</navnelinje>
                                <navnelinje>$navnLinje4</navnelinje>
                                <navnelinje>$navnLinje5</navnelinje>
                            </navn>
                            <redigertNavn>$navnLinje1</redigertNavn>
                        </navn>
                        <formaal fomBruksperiode="2016-04-22T04:04:36+02:00" fomGyldighetsperiode="2016-04-21T00:00:00.000+02:00">
                            <formaal>Stortinget</formaal>
                        </formaal>
                        <organisasjonEnhetstyper fomBruksperiode="2014-05-21T20:06:47.986+02:00" fomGyldighetsperiode="1995-08-09T00:00:00.000+02:00">
                            <enhetstype>Staten (STAT)</enhetstype>
                        </organisasjonEnhetstyper>
                    </organisasjonDetaljer>
                </organisasjon>
            </response>
        </ns2:hentOrganisasjonResponse>
    </soap:Body>
</soap:Envelope>
""".trimIndent()

private fun faultXml(fault: String) = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body>
        <soap:Fault>
            <faultcode xmlns:ns1="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">soap:Server</faultcode>
            <faultstring>$fault</faultstring>
        </soap:Fault>
    </soap:Body>
</soap:Envelope>
""".trimIndent()

private fun virksomhetFinnesIkke(orgNr: String) = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body>
        <ns2:hentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/organisasjon/v5">
            <response>
                <unntakForOrgnrListe>
                    <unntaksmelding>$orgNr er opphørt eller eksisterer ikke på dato 2019-01-01</unntaksmelding>
                    <organisasjonsnummer>$orgNr</organisasjonsnummer>
                </unntakForOrgnrListe>
            </response>
        </ns2:hentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse>
    </soap:Body>
</soap:Envelope>
""".trimIndent()
