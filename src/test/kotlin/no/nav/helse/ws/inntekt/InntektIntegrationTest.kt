package no.nav.helse.ws.inntekt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.ktor.http.HttpStatusCode
import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.sts.StsRestClient
import no.nav.helse.ws.Fødselsnummer
import no.nav.helse.ws.WsClients
import no.nav.helse.ws.samlAssertionResponse
import no.nav.helse.ws.sts.stsClient
import no.nav.helse.ws.stsStub
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.YearMonth
import kotlin.test.assertEquals

class InntektIntegrationTest {

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
        val fnr = Fødselsnummer("12345678911")
        val fom = YearMonth.parse("2017-01")
        val tom = YearMonth.parse("2019-01")

        val filter = "ForeldrepengerA-Inntekt"
        val formål = "Foreldrepenger"

        inntektStub(
                server = server,
                scenario = "inntektskomponenten_feil",
                request = hentInntektListeBolkStub(fnr.value, "2017-01Z", "2019-01Z", filter, formål),
                response = WireMock.serverError().withBody(hentInntektListeBolk_fault_response)
        ) { inntektClient ->
            val actual = inntektClient.hentInntektListe(fnr, fom, tom)

            when (actual) {
                is OppslagResult.Feil -> {
                    assertEquals(HttpStatusCode.InternalServerError, actual.httpCode)
                    when (actual.feil) {
                        is Feil.Exception -> {
                            assertEquals("SOAP fault", (actual.feil as Feil.Exception).feilmelding)
                            assertEquals("javax.xml.ws.soap.SOAPFaultException", (actual.feil as Feil.Exception).exception.javaClass.name)
                        }
                        else -> fail { "Expected Feil.Exception to be returned" }
                    }
                }
                is OppslagResult.Ok -> fail { "Expected OppslagResult.Feil to be returned" }
            }
        }
    }

    @Test
    fun `skal svare med liste over inntekter`() {
        val fnr = Fødselsnummer("12345678911")
        val fom = YearMonth.parse("2017-01")
        val tom = YearMonth.parse("2019-01")

        val filter = "ForeldrepengerA-Inntekt"
        val formål = "Foreldrepenger"

        inntektStub(
                server = server,
                scenario = "inntektskomponenten_feil",
                request = hentInntektListeBolkStub(fnr.value, "2017-01Z", "2019-01Z", filter, formål),
                response = WireMock.okXml(hentInntektListeBolk_response)
        ) { inntektClient ->
            val actual = inntektClient.hentInntektListe(fnr, fom, tom)

            when (actual) {
                is OppslagResult.Ok -> {
                    assertEquals(1, actual.data.arbeidsInntektIdentListe.size)
                    assertEquals(2, actual.data.arbeidsInntektIdentListe[0].arbeidsInntektMaaned.size)
                }
                is OppslagResult.Feil -> fail { "Expected OppslagResult.Ok to be returned" }
            }
        }
    }
}

fun inntektStub(server: WireMockServer, scenario: String, request: MappingBuilder, response: ResponseDefinitionBuilder, test: (InntektClient) -> Unit) {
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
            .willSetStateTo("inntektskomponenten_stub_called"))

    val stsClientWs = stsClient(server.baseUrl().plus("/sts"), stsUsername to stsPassword)
    val stsClientRest = StsRestClient(server.baseUrl().plus("/sts"), stsUsername, stsPassword)

    val wsClients = WsClients(stsClientWs, stsClientRest, true)

    test(wsClients.inntekt(server.baseUrl().plus("/inntekt")))

    WireMock.listAllStubMappings().mappings.forEach {
        WireMock.verify(RequestPatternBuilder.like(it.request))
    }
}

private val hentInntektListeBolk_fault_response = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body>
        <soap:Fault>
            <faultcode xmlns:ns1="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">soap:Server</faultcode>
            <faultstring>SOAP fault</faultstring>
        </soap:Fault>
    </soap:Body>
</soap:Envelope>
""".trimIndent()

private val hentInntektListeBolk_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:hentInntektListeBolkResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/inntekt/v3">
         <response>
            <arbeidsInntektIdentListe>
               <arbeidsInntektMaaned>
                  <aarMaaned>2017-12+01:00</aarMaaned>
                  <arbeidsInntektInformasjon>
                     <inntektListe xsi:type="ns4:Loennsinntekt" xmlns:ns4="http://nav.no/tjeneste/virksomhet/inntekt/v3/informasjon/inntekt" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                        <beloep>40000</beloep>
                        <fordel>kontantytelse</fordel>
                        <inntektskilde>A-ordningen</inntektskilde>
                        <inntektsperiodetype>Maaned</inntektsperiodetype>
                        <inntektsstatus>LoependeInnrapportert</inntektsstatus>
                        <levereringstidspunkt>2018-12-05T09:50:10.777+01:00</levereringstidspunkt>
                        <utbetaltIPeriode>2017-12</utbetaltIPeriode>
                        <opplysningspliktig xsi:type="ns4:Organisasjon">
                           <orgnummer>973861778</orgnummer>
                        </opplysningspliktig>
                        <virksomhet xsi:type="ns4:Organisasjon">
                           <orgnummer>973861778</orgnummer>
                        </virksomhet>
                        <inntektsmottaker xsi:type="ns4:PersonIdent">
                           <personIdent>13119924167</personIdent>
                        </inntektsmottaker>
                        <inngaarIGrunnlagForTrekk>true</inngaarIGrunnlagForTrekk>
                        <utloeserArbeidsgiveravgift>true</utloeserArbeidsgiveravgift>
                        <informasjonsstatus>InngaarAlltid</informasjonsstatus>
                        <beskrivelse>fastloenn</beskrivelse>
                     </inntektListe>
                  </arbeidsInntektInformasjon>
               </arbeidsInntektMaaned>
               <arbeidsInntektMaaned>
                  <aarMaaned>2018-01+01:00</aarMaaned>
                  <arbeidsInntektInformasjon>
                     <inntektListe xsi:type="ns4:Loennsinntekt" xmlns:ns4="http://nav.no/tjeneste/virksomhet/inntekt/v3/informasjon/inntekt" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                        <beloep>40000</beloep>
                        <fordel>kontantytelse</fordel>
                        <inntektskilde>A-ordningen</inntektskilde>
                        <inntektsperiodetype>Maaned</inntektsperiodetype>
                        <inntektsstatus>LoependeInnrapportert</inntektsstatus>
                        <levereringstidspunkt>2018-12-05T09:50:10.777+01:00</levereringstidspunkt>
                        <utbetaltIPeriode>2018-01</utbetaltIPeriode>
                        <opplysningspliktig xsi:type="ns4:Organisasjon">
                           <orgnummer>973861778</orgnummer>
                        </opplysningspliktig>
                        <virksomhet xsi:type="ns4:Organisasjon">
                           <orgnummer>973861778</orgnummer>
                        </virksomhet>
                        <inntektsmottaker xsi:type="ns4:PersonIdent">
                           <personIdent>13119924167</personIdent>
                        </inntektsmottaker>
                        <inngaarIGrunnlagForTrekk>true</inngaarIGrunnlagForTrekk>
                        <utloeserArbeidsgiveravgift>true</utloeserArbeidsgiveravgift>
                        <informasjonsstatus>InngaarAlltid</informasjonsstatus>
                        <beskrivelse>fastloenn</beskrivelse>
                     </inntektListe>
                  </arbeidsInntektInformasjon>
               </arbeidsInntektMaaned>
               <ident xsi:type="ns4:PersonIdent" xmlns:ns4="http://nav.no/tjeneste/virksomhet/inntekt/v3/informasjon/inntekt" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <personIdent>13119924167</personIdent>
               </ident>
            </arbeidsInntektIdentListe>
         </response>
      </ns2:hentInntektListeBolkResponse>
   </soap:Body>
</soap:Envelope>
""".trimIndent()
