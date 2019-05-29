package no.nav.helse.oppslag.infotrygdberegningsgrunnlag

import arrow.core.Try
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.stubbing.Scenario
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.oppslag.*
import no.nav.helse.oppslag.sts.stsClient
import no.nav.helse.sts.StsRestClient
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Organisasjonsenhet
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate

class InfotrygdBeregningsgrunnlagIntegrationTest {

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
    fun `skal håndtere at basene i infotrygd er utilgjengelige`() {
        val fnr = "11111111111"
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now()

        val request = WireMock.post(WireMock.urlPathEqualTo("/infotrygd"))
                .withSoapAction("http://nav.no/tjeneste/virksomhet/infotrygdBeregningsgrunnlag/v1/infotrygdBeregningsgrunnlag_v1/finnGrunnlagListeRequest")
                .withRequestBody(ContainsPattern("<personident>$fnr</personident>"))

        infotrygdStub(
                server = server,
                scenario = "infotrygd_finn_grunnlag_liste",
                response = WireMock.okXml(basene_i_infotrygd_er_utilgjengelige),
                request = request
        ) { infotrygdBeregningsgrunnlagClient ->
            val expected = listOf(Organisasjonsenhet().apply {
                this.enhetId = enhetId
                this.enhetNavn = enhetNavn
            })
            val actual = infotrygdBeregningsgrunnlagClient.finnGrunnlagListe(Fødselsnummer(fnr), fom, tom)

            when (actual) {
                is Try.Failure -> assertTrue(actual.exception is BaseneErUtilgjengeligeException)
                else -> fail { "Expected Try.Failure to be returned" }
            }
        }
    }
}

private fun infotrygdStub(server: WireMockServer, scenario: String, response: ResponseDefinitionBuilder, request: MappingBuilder, test: (InfotrygdBeregningsgrunnlagClient) -> Unit) {
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
            .willSetStateTo("infotrygd_stub_called"))

    val stsClientWs = stsClient(server.baseUrl().plus("/sts"), stsUsername to stsPassword)
    val stsClientRest = StsRestClient(server.baseUrl().plus("/sts"), stsUsername, stsPassword)

    val wsClients = WsClients(stsClientWs, stsClientRest, true)

    test(wsClients.infotrygdBeregningsgrunnlag(server.baseUrl().plus("/infotrygd")))

    WireMock.listAllStubMappings().mappings.forEach {
        WireMock.verify(RequestPatternBuilder.like(it.request))
    }
}

private val basene_i_infotrygd_er_utilgjengelige = """
<?xml version='1.0' encoding='UTF-8'?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Header>
        <Action xmlns="http://www.w3.org/2005/08/addressing">
            http://nav.no/tjeneste/virksomhet/infotrygdBeregningsgrunnlag/v1/infotrygdBeregningsgrunnlag_v1/finnGrunnlagListe/Fault/SOAPFaultException
        </Action>
    </soap:Header>
    <soap:Body>
        <soap:Fault>
            <faultcode>soap:Client</faultcode>
            <faultstring>Basene i Infotrygd er ikke tilgjengelige</faultstring>
            <detail></detail>
        </soap:Fault>
    </soap:Body>
</soap:Envelope>
""".trimIndent()
