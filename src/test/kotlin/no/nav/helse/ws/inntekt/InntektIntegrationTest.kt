package no.nav.helse.ws.inntekt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.stubbing.Scenario
import no.nav.helse.Either
import no.nav.helse.sts.StsRestClient
import no.nav.helse.ws.AktørId
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
import javax.xml.ws.soap.SOAPFaultException
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
        val aktørId = AktørId("12345678911")
        val fom = YearMonth.parse("2017-01")
        val tom = YearMonth.parse("2019-01")

        val filter = "8-28"
        val formål = "Foreldrepenger"

        inntektStub(
                server = server,
                scenario = "inntektskomponenten_feil",
                request = hentInntektListeBolkStub(aktørId.aktor, "2017-01Z", "2019-01Z", filter, formål),
                response = WireMock.serverError().withBody(hentInntektListeBolk_fault_response)
        ) { inntektClient ->
            val actual = inntektClient.hentBeregningsgrunnlag(aktørId, fom, tom)

            when (actual) {
                is Either.Left -> {
                    when (actual.left) {
                        is SOAPFaultException -> assertEquals("SOAP fault", actual.left.message)
                        else -> fail { "Expected SOAPFaultException to be returned" }
                    }
                }
                is Either.Right -> fail { "Expected Either.Left to be returned" }
            }
        }
    }

    @Test
    fun `skal svare med liste over inntekter`() {
        val aktørId = AktørId("12345678911")
        val fom = YearMonth.parse("2017-01")
        val tom = YearMonth.parse("2019-01")

        val filter = "8-28"
        val formål = "Foreldrepenger"

        inntektStub(
                server = server,
                scenario = "inntektskomponenten_feil",
                request = hentInntektListeBolkStub(aktørId.aktor, "2017-01Z", "2019-01Z", filter, formål),
                response = WireMock.okXml(hentInntektListeBolk_response)
        ) { inntektClient ->
            val actual = inntektClient.hentBeregningsgrunnlag(aktørId, fom, tom)

            when (actual) {
                is Either.Right -> {
                    assertEquals(1, actual.right.arbeidsInntektIdentListe.size)
                    assertEquals(2, actual.right.arbeidsInntektIdentListe[0].arbeidsInntektMaaned.size)
                }
                is Either.Left -> fail { "Expected Either.Right to be returned" }
            }
        }
    }

    @Test
    fun `skal svare med feil når tjenesten svarer med feil for sammenligningsgrunnlag`() {
        val aktørId = AktørId("12345678911")
        val fom = YearMonth.parse("2017-01")
        val tom = YearMonth.parse("2019-01")

        val filter = "8-30"
        val formål = "Foreldrepenger"

        inntektStub(
                server = server,
                scenario = "inntektskomponenten_feil",
                request = hentInntektListeBolkStub(aktørId.aktor, "2017-01Z", "2019-01Z", filter, formål),
                response = WireMock.serverError().withBody(hentInntektListeBolk_fault_response)
        ) { inntektClient ->
            val actual = inntektClient.hentSammenligningsgrunnlag(aktørId, fom, tom)

            when (actual) {
                is Either.Left -> {
                    when (actual.left) {
                        is SOAPFaultException -> assertEquals("SOAP fault", actual.left.message)
                        else -> fail { "Expected SOAPFaultException to be returned" }
                    }
                }
                is Either.Right -> fail { "Expected Either.Left to be returned" }
            }
        }
    }

    @Test
    fun `skal svare med liste over inntekter for sammenligningsgrunnlag`() {
        val aktørId = AktørId("12345678911")
        val fom = YearMonth.parse("2017-01")
        val tom = YearMonth.parse("2019-01")

        val filter = "8-30"
        val formål = "Foreldrepenger"

        inntektStub(
                server = server,
                scenario = "inntektskomponenten_feil",
                request = hentInntektListeBolkStub(aktørId.aktor, "2017-01Z", "2019-01Z", filter, formål),
                response = WireMock.okXml(hentInntektListeBolk_response)
        ) { inntektClient ->
            val actual = inntektClient.hentSammenligningsgrunnlag(aktørId, fom, tom)

            when (actual) {
                is Either.Right -> {
                    assertEquals(1, actual.right.arbeidsInntektIdentListe.size)
                    assertEquals(2, actual.right.arbeidsInntektIdentListe[0].arbeidsInntektMaaned.size)
                }
                is Either.Left -> fail { "Expected Either.Right to be returned" }
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
                        <inntektsmottaker xsi:type="ns4:AktoerId">
                           <aktoerId>13119924167</aktoerId>
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
                        <inntektsmottaker xsi:type="ns4:AktoerId">
                           <aktoerId>13119924167</aktoerId>
                        </inntektsmottaker>
                        <inngaarIGrunnlagForTrekk>true</inngaarIGrunnlagForTrekk>
                        <utloeserArbeidsgiveravgift>true</utloeserArbeidsgiveravgift>
                        <informasjonsstatus>InngaarAlltid</informasjonsstatus>
                        <beskrivelse>fastloenn</beskrivelse>
                     </inntektListe>
                  </arbeidsInntektInformasjon>
               </arbeidsInntektMaaned>
               <ident xsi:type="ns4:AktoerId" xmlns:ns4="http://nav.no/tjeneste/virksomhet/inntekt/v3/informasjon/inntekt" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <aktoerId>13119924167</aktoerId>
               </ident>
            </arbeidsInntektIdentListe>
         </response>
      </ns2:hentInntektListeBolkResponse>
   </soap:Body>
</soap:Envelope>
""".trimIndent()
