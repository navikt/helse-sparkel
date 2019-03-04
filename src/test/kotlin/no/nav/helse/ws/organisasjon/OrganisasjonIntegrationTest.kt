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
                .withSoapAction("http://nav.no/tjeneste/virksomhet/organisasjon/v5/BindinghentNoekkelinfoOrganisasjon/")
                .withRequestBody(ContainsPattern("<orgnummer>$orgNr</orgnummer>"))

        organisasjonStub(
                server = server,
                scenario = "organisasjon_hent_navn",
                request = requestStub,
                response = WireMock.serverError().withBody(faultXml("SOAP fault"))
        ) { organisasjonClient ->
            val actual = organisasjonClient.hentOrganisasjon(OrganisasjonsNummer(orgNr),
                    listOf(OrganisasjonsAttributt("navn")))

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
                .withSoapAction("http://nav.no/tjeneste/virksomhet/organisasjon/v5/BindinghentNoekkelinfoOrganisasjon/")
                .withRequestBody(ContainsPattern("<orgnummer>$orgNr</orgnummer>"))

        organisasjonStub(
                server = server,
                scenario = "organisasjon_hent_navn",
                request = requestStub,
                response = WireMock.okXml(okXml(
                        orgNr = orgNr,
                        navnLinje1 = "NAV",
                        navnLinje2 = "AVD SANNERGATA 2"
                ))
        ) { organisasjonClient ->
            val actual = organisasjonClient.hentOrganisasjon(OrganisasjonsNummer(orgNr),
                    listOf(OrganisasjonsAttributt("navn")))

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
                .withSoapAction("http://nav.no/tjeneste/virksomhet/organisasjon/v5/BindinghentNoekkelinfoOrganisasjon/")
                .withRequestBody(ContainsPattern("<orgnummer>$orgNr</orgnummer>"))

        organisasjonStub(
                server = server,
                scenario = "organisasjon_hent_navn",
                request = requestStub,
                response = WireMock.okXml(okXml(
                        orgNr = orgNr
                ))
        ) { organisasjonClient ->
            val actual = organisasjonClient.hentOrganisasjon(OrganisasjonsNummer(orgNr),
                    listOf(OrganisasjonsAttributt("navn")))

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
        <ns2:hentNoekkelinfoOrganisasjonResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/organisasjon/v5">
            <response>
                <orgnummer>$orgNr</orgnummer>
                <navn xmlns:ns4="http://nav.no/tjeneste/virksomhet/organisasjon/v5/informasjon" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns4:UstrukturertNavn">
                    <navnelinje>$navnLinje1</navnelinje>
                    <navnelinje>$navnLinje2</navnelinje>
                    <navnelinje>$navnLinje3</navnelinje>
                    <navnelinje>$navnLinje4</navnelinje>
                    <navnelinje>$navnLinje5</navnelinje>
                </navn>
                <adresse xmlns:ns4="http://nav.no/tjeneste/virksomhet/organisasjon/v5/informasjon" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" fomBruksperiode="2015-02-23T10:38:34.403+01:00" fomGyldighetsperiode="2013-07-09T00:00:00.000+02:00" xsi:type="ns4:SemistrukturertAdresse">
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
                </adresse>
                <enhetstype kodeRef="STAT"/>
            </response>
        </ns2:hentNoekkelinfoOrganisasjonResponse>
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
