package no.nav.helse.ws.arbeidsforhold

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.helse.assertJsonEquals
import no.nav.helse.bootstrapComponentTest
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.http.aktør.AktørregisterService
import no.nav.helse.http.aktør.aktørregisterStub
import no.nav.helse.mockedSparkel
import no.nav.helse.sts.StsRestClient
import no.nav.helse.ws.WsClients
import no.nav.helse.ws.organisasjon.OrganisasjonMocks
import no.nav.helse.ws.organisasjon.OrganisasjonService
import no.nav.helse.ws.sts.stsClient
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class ArbeidsforholdComponentTest {

    companion object {
        val bootstrap = bootstrapComponentTest()

        @BeforeAll
        @JvmStatic
        fun start() {
            bootstrap.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            bootstrap.stop()
        }
    }


    @AfterEach
    @BeforeEach
    fun `clear server`() {
        bootstrap.reset()
    }

    @Test
    fun `en liste over arbeidsgivere skal returneres`() {
        val token = bootstrap.jwkStub.createTokenFor("srvpleiepengesokna")

        WireMock.stubFor(aktørregisterStub("1831212532188", "1831212532188", "08088806280"))

        WireMock.stubFor(finnArbeidsforholdPrArbeidstakerStub("08088806280",
                LocalDate.parse("2017-01-01").toXmlGregorianCalendar().toXMLFormat(), LocalDate.parse("2019-01-01").toXmlGregorianCalendar().toXMLFormat())
                .withSamlAssertion()
                .withCallId()
                .inScenario("aareg")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("arbeidsforhold_hentet")
                .willReturn(WireMock.okXml(finnArbeidsforholdPrArbeidstaker_response)))

        val org1Responses = OrganisasjonMocks.okResponses(
                orgNr = "913548221",
                navnLinje1 = "EQUINOR AS",
                navnLinje2 = "AVD STATOIL SOKKELVIRKSOMHET"
        )
        OrganisasjonMocks.mock(
                orgNr = "913548221",
                xml = org1Responses.registerXmlResponse
        )

        val org2Responses = OrganisasjonMocks.okResponses(
                orgNr = "984054564",
                navnLinje1 = "NAV",
                navnLinje2 = "AVD WALDEMAR THRANES GATE"
        )
        OrganisasjonMocks.mock(
                orgNr = "984054564",
                xml = org2Responses.registerXmlResponse
        )

        val stsClientWs = stsClient(bootstrap.env.securityTokenServiceEndpointUrl,
                bootstrap.env.securityTokenUsername to bootstrap.env.securityTokenPassword)
        val stsClientRest = StsRestClient(
                bootstrap.env.stsRestUrl, bootstrap.env.securityTokenUsername, bootstrap.env.securityTokenPassword)

        val wsClients = WsClients(stsClientWs, stsClientRest, bootstrap.env.allowInsecureSoapRequests)

        withTestApplication({mockedSparkel(
                jwtIssuer = bootstrap.env.jwtIssuer,
                jwkProvider = bootstrap.jwkStub.stubbedJwkProvider(),
                arbeidsforholdService = ArbeidsforholdService(
                        arbeidsforholdClient = wsClients.arbeidsforhold(bootstrap.env.arbeidsforholdEndpointUrl),
                        aktørregisterService = AktørregisterService(wsClients.aktør(bootstrap.env.aktørregisterUrl)),
                        organisasjonService = OrganisasjonService(wsClients.organisasjon(bootstrap.env.organisasjonEndpointUrl))
                ))}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsforhold/1831212532188?fom=2017-01-01&tom=2019-01-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(200, response.status()?.value)
                assertJsonEquals(JSONObject(expectedJson), JSONObject(response.content))
            }
        }
    }
}

private val finnArbeidsforholdPrArbeidstaker_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:finnArbeidsforholdPrArbeidstakerResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3">
         <parameters>
            <arbeidsforhold applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-01-05T13:31:11.214+01:00" opphavREF="eda00000-0000-0000-0000-000972417562" opprettelsestidspunkt="2015-10-05T14:00:16.809+02:00" opprettetAv="srvappserver" sistBekreftet="2018-01-05T13:29:41.000+01:00">
               <arbeidsforholdID>00000009692000000001</arbeidsforholdID>
               <arbeidsforholdIDnav>38009429</arbeidsforholdIDnav>
               <ansettelsesPeriode applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-01-05T13:31:11.177+01:00" fomBruksperiode="2018-01-05+01:00" opphavREF="eda00000-0000-0000-0000-000972417562">
                  <periode>
                     <fom>2015-09-19T00:00:00.000+02:00</fom>
                     <tom>2017-11-30T00:00:00.000+01:00</tom>
                  </periode>
               </ansettelsesPeriode>
               <arbeidsforholdstype kodeRef="ordinaertArbeidsforhold">Ordinært arbeidsforhold</arbeidsforholdstype>
               <antallTimerForTimeloennet applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2015-10-05T14:00:16.809+02:00" opphavREF="06c8a5b7-2d59-44fd-9f98-79cc4fc44a65">
                  <periode>
                     <fom>2015-09-01T00:00:00.000+02:00</fom>
                     <tom>2015-09-30T00:00:00.000+02:00</tom>
                  </periode>
                  <antallTimer>2.0</antallTimer>
                  <rapporteringsperiode>2015-09+02:00</rapporteringsperiode>
               </antallTimerForTimeloennet>
               <antallTimerForTimeloennet applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2015-11-05T12:48:16.406+01:00" opphavREF="35e5241a-425e-44b2-b32e-50c17bac570e">
                  <periode>
                     <fom>2015-10-01T00:00:00.000+02:00</fom>
                     <tom>2015-10-31T00:00:00.000+01:00</tom>
                  </periode>
                  <antallTimer>15.8</antallTimer>
                  <rapporteringsperiode>2015-10+02:00</rapporteringsperiode>
               </antallTimerForTimeloennet>
               <antallTimerForTimeloennet applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2015-12-04T15:23:27.334+01:00" opphavREF="092da794-e540-48af-85de-05b66c330138">
                  <periode>
                     <fom>2015-11-01T00:00:00.000+01:00</fom>
                     <tom>2015-11-30T00:00:00.000+01:00</tom>
                  </periode>
                  <antallTimer>8.0</antallTimer>
                  <rapporteringsperiode>2015-11+01:00</rapporteringsperiode>
               </antallTimerForTimeloennet>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-01-05T13:31:11.177+01:00" fomBruksperiode="2018-01-05+01:00" fomGyldighetsperiode="2017-12-01T00:00:00.000+01:00" opphavREF="eda00000-0000-0000-0000-000972417562">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>100.0</stillingsprosent>
                  <sisteLoennsendringsdato>2017-12-01+01:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>37.5</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsgiver xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>913548221</orgnummer>
               </arbeidsgiver>
               <arbeidstaker>
                  <ident>
                     <ident>07067625948</ident>
                  </ident>
               </arbeidstaker>
               <opplysningspliktig xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>913548221</orgnummer>
               </opplysningspliktig>
               <arbeidsforholdInnrapportertEtterAOrdningen>true</arbeidsforholdInnrapportertEtterAOrdningen>
            </arbeidsforhold>
            <arbeidsforhold applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-05-08T11:24:25.569+02:00" opphavREF="eda00000-0000-0000-0000-001190431060" opprettelsestidspunkt="2018-02-07T11:44:40.548+01:00" opprettetAv="srvappserver" sistBekreftet="2018-05-08T11:13:39.000+02:00">
               <arbeidsforholdID>00000009692000000005</arbeidsforholdID>
               <arbeidsforholdIDnav>44669403</arbeidsforholdIDnav>
               <ansettelsesPeriode applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-02-07T11:44:40.548+01:00" fomBruksperiode="2018-02-07+01:00" opphavREF="eda00000-0000-0000-0000-001041599767">
                  <periode>
                     <fom>2015-09-19T00:00:00.000+02:00</fom>
                     <tom>2017-10-31T00:00:00.000+01:00</tom>
                  </periode>
               </ansettelsesPeriode>
               <arbeidsforholdstype kodeRef="ordinaertArbeidsforhold">Ordinært arbeidsforhold</arbeidsforholdstype>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-02-07T11:44:40.548+01:00" fomBruksperiode="2018-02-07+01:00" fomGyldighetsperiode="2018-01-01T00:00:00.000+01:00" opphavREF="eda00000-0000-0000-0000-001041599767">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>0.0</stillingsprosent>
                  <sisteLoennsendringsdato>2015-09-19+02:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>0.0</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsgiver xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>984054564</orgnummer>
               </arbeidsgiver>
               <arbeidstaker>
                  <ident>
                     <ident>07067625948</ident>
                  </ident>
               </arbeidstaker>
               <opplysningspliktig xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>984054564</orgnummer>
               </opplysningspliktig>
               <arbeidsforholdInnrapportertEtterAOrdningen>true</arbeidsforholdInnrapportertEtterAOrdningen>
            </arbeidsforhold>
         </parameters>
      </ns2:finnArbeidsforholdPrArbeidstakerResponse>
   </soap:Body>
</soap:Envelope>
""".trimIndent()

private val expectedJson = """
    {
        "organisasjoner": [{
            "organisasjonsnummer": "913548221",
            "navn": "EQUINOR AS, AVD STATOIL SOKKELVIRKSOMHET"
        },{
            "organisasjonsnummer": "984054564",
            "navn": "NAV, AVD WALDEMAR THRANES GATE"
        }]
    }
""".trimIndent()
