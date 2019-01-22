package no.nav.helse.ws.arbeidsforhold

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.prometheus.client.CollectorRegistry
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.helse.*
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Ignore
import org.junit.jupiter.api.*
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
    fun `clear server`() {
        bootstrap.reset()
    }

    @BeforeEach
    fun `clear prometheus registry before test`() {
        CollectorRegistry.defaultRegistry.clear()
    }

    @AfterEach
    fun `clear prometheus registry after test`() {
        CollectorRegistry.defaultRegistry.clear()
    }

    @Test
    fun `that response is json`() {
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

        WireMock.stubFor(hentArbeidsforholdHistorikkStub("45526756")
                .withSamlAssertion()
                .withCallId()
                .inScenario("aareg")
                .whenScenarioStateIs("arbeidsforhold_hentet")
                .willSetStateTo("første_arbeidsforholdhistorikk_hentet")
                .willReturn(WireMock.okXml(hentArbeidsforholdHistorikk_response1)))

        WireMock.stubFor(hentArbeidsforholdHistorikkStub("45526864")
                .withSamlAssertion()
                .withCallId()
                .inScenario("aareg")
                .whenScenarioStateIs("første_arbeidsforholdhistorikk_hentet")
                .willSetStateTo("andre_arbeidsforholdhistorikk_hentet")
                .willReturn(WireMock.okXml(hentArbeidsforholdHistorikk_response2)))

        withTestApplication({sparkel(bootstrap.env, bootstrap.jwkStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsforhold/1831212532188?fom=2017-01-01&tom=2019-01-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(200, response.status()?.value)
                assertJsonEquals(JSONArray(expectedJson), JSONArray(response.content))
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
            <arbeidsforhold applikasjonsID="AAREG" endretAv="Z991012" endringstidspunkt="2018-10-30T11:26:37.029+01:00" opphavREF="3" opprettelsestidspunkt="2018-10-30T11:26:37.029+01:00" opprettetAv="Z991012" sistBekreftet="2018-10-30T11:26:36.000+01:00">
               <arbeidsforholdID>1</arbeidsforholdID>
               <arbeidsforholdIDnav>45526756</arbeidsforholdIDnav>
               <ansettelsesPeriode applikasjonsID="AAREG" endretAv="Z991012" endringstidspunkt="2018-10-30T11:26:37.029+01:00" fomBruksperiode="2018-10-30+01:00" opphavREF="3">
                  <periode>
                     <fom>2017-04-01T00:00:00.000+02:00</fom>
                  </periode>
               </ansettelsesPeriode>
               <arbeidsforholdstype kodeRef="ordinaertArbeidsforhold">Ordinært arbeidsforhold</arbeidsforholdstype>
               <arbeidsavtale applikasjonsID="AAREG" endretAv="Z991012" endringstidspunkt="2018-10-30T11:26:37.029+01:00" fomBruksperiode="2018-10-30+01:00" fomGyldighetsperiode="2017-04-01T00:00:00.000+02:00" opphavREF="3">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="fast">Fastlønn</avloenningstype>
                  <yrke kodeRef="3431101">ADMINISTRASJONSSEKRETÆR</yrke>
                  <avtaltArbeidstimerPerUke>40.0</avtaltArbeidstimerPerUke>
                  <stillingsprosent>100.0</stillingsprosent>
                  <beregnetAntallTimerPrUke>40.0</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsgiver xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>910831143</orgnummer>
                  <navn>Maxbo</navn>
               </arbeidsgiver>
               <arbeidstaker>
                  <ident>
                     <ident>08088806280</ident>
                  </ident>
               </arbeidstaker>
               <opplysningspliktig xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>910970046</orgnummer>
               </opplysningspliktig>
               <arbeidsforholdInnrapportertEtterAOrdningen>true</arbeidsforholdInnrapportertEtterAOrdningen>
            </arbeidsforhold>
            <arbeidsforhold applikasjonsID="AAREG" endretAv="Z991012" endringstidspunkt="2018-10-30T11:38:54.966+01:00" opphavREF="3" opprettelsestidspunkt="2018-10-30T11:38:54.966+01:00" opprettetAv="Z991012" sistBekreftet="2018-10-30T11:38:54.000+01:00">
               <arbeidsforholdID>147</arbeidsforholdID>
               <arbeidsforholdIDnav>45526864</arbeidsforholdIDnav>
               <ansettelsesPeriode applikasjonsID="AAREG" endretAv="Z991012" endringstidspunkt="2018-10-30T11:38:54.966+01:00" fomBruksperiode="2018-10-30+01:00" opphavREF="3">
                  <periode>
                     <fom>2017-04-01T00:00:00.000+02:00</fom>
                  </periode>
               </ansettelsesPeriode>
               <arbeidsforholdstype kodeRef="ordinaertArbeidsforhold">Ordinært arbeidsforhold</arbeidsforholdstype>
               <arbeidsavtale applikasjonsID="AAREG" endretAv="Z991012" endringstidspunkt="2018-10-30T11:38:54.966+01:00" fomBruksperiode="2018-10-30+01:00" fomGyldighetsperiode="2017-04-01T00:00:00.000+02:00" opphavREF="3">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="fast">Fastlønn</avloenningstype>
                  <yrke kodeRef="3431101">ADMINISTRASJONSSEKRETÆR</yrke>
                  <avtaltArbeidstimerPerUke>40.0</avtaltArbeidstimerPerUke>
                  <stillingsprosent>100.0</stillingsprosent>
                  <beregnetAntallTimerPrUke>40.0</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsgiver xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>973861778</orgnummer>
                  <navn>Telenor</navn>
               </arbeidsgiver>
               <arbeidstaker>
                  <ident>
                     <ident>08088806280</ident>
                  </ident>
               </arbeidstaker>
               <opplysningspliktig xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>923609016</orgnummer>
               </opplysningspliktig>
               <arbeidsforholdInnrapportertEtterAOrdningen>true</arbeidsforholdInnrapportertEtterAOrdningen>
            </arbeidsforhold>
         </parameters>
      </ns2:finnArbeidsforholdPrArbeidstakerResponse>
   </soap:Body>
</soap:Envelope>
""".trimIndent()

private val hentArbeidsforholdHistorikk_response1 = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:hentArbeidsforholdHistorikkResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3">
         <parameters>
            <arbeidsforhold applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-12-18T13:30:19.220+01:00" opphavREF="eda00000-0000-0000-0000-001548223178" opprettelsestidspunkt="2018-07-03T21:05:50.328+02:00" opprettetAv="srvappserver" sistBekreftet="2018-12-18T13:19:28.000+01:00">
               <arbeidsforholdID>20-26</arbeidsforholdID>
               <arbeidsforholdIDnav>45526756</arbeidsforholdIDnav>
               <ansettelsesPeriode applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-09-20T13:51:55.901+02:00" fomBruksperiode="2018-09-20+02:00" opphavREF="eda00000-0000-0000-0000-001393469929">
                  <periode>
                     <fom>2018-05-01T00:00:00.000+02:00</fom>
                     <tom>2018-08-31T00:00:00.000+02:00</tom>
                  </periode>
               </ansettelsesPeriode>
               <arbeidsforholdstype kodeRef="ordinaertArbeidsforhold">Ordinært arbeidsforhold</arbeidsforholdstype>
               <antallTimerForTimeloennet applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-07-03T21:05:50.328+02:00" opphavREF="eda00000-0000-0000-0000-001280332174">
                  <periode>
                     <fom>2018-06-01T00:00:00.000+02:00</fom>
                     <tom>2018-06-30T00:00:00.000+02:00</tom>
                  </periode>
                  <antallTimer>119.0</antallTimer>
                  <rapporteringsperiode>2018-06+02:00</rapporteringsperiode>
               </antallTimerForTimeloennet>
               <antallTimerForTimeloennet applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-07-26T14:04:36.524+02:00" opphavREF="eda00000-0000-0000-0000-001306788133">
                  <periode>
                     <fom>2018-07-01T00:00:00.000+02:00</fom>
                     <tom>2018-07-31T00:00:00.000+02:00</tom>
                  </periode>
                  <antallTimer>28.5</antallTimer>
                  <rapporteringsperiode>2018-07+02:00</rapporteringsperiode>
               </antallTimerForTimeloennet>
               <antallTimerForTimeloennet applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-09-04T20:56:19.942+02:00" opphavREF="eda00000-0000-0000-0000-001375455402">
                  <periode>
                     <fom>2018-08-01T00:00:00.000+02:00</fom>
                     <tom>2018-08-31T00:00:00.000+02:00</tom>
                  </periode>
                  <antallTimer>165.0</antallTimer>
                  <rapporteringsperiode>2018-08+02:00</rapporteringsperiode>
               </antallTimerForTimeloennet>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-07-03T21:05:50.328+02:00" fomBruksperiode="2018-07-03+02:00" fomGyldighetsperiode="2018-06-01T00:00:00.000+02:00" opphavREF="eda00000-0000-0000-0000-001280332174">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <yrke kodeRef="9160118">GJENVINNINGSARBEIDER</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>25.0</stillingsprosent>
                  <sisteLoennsendringsdato>2018-05-01+02:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>9.375</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsgiver xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>914961726</orgnummer>
               </arbeidsgiver>
               <arbeidstaker>
                  <ident>
                     <ident>66108420104</ident>
                  </ident>
               </arbeidstaker>
               <opplysningspliktig xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>914491533</orgnummer>
               </opplysningspliktig>
               <arbeidsforholdInnrapportertEtterAOrdningen>true</arbeidsforholdInnrapportertEtterAOrdningen>
            </arbeidsforhold>
         </parameters>
      </ns2:hentArbeidsforholdHistorikkResponse>
   </soap:Body>
</soap:Envelope>
""".trimIndent()

private val hentArbeidsforholdHistorikk_response2 = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:hentArbeidsforholdHistorikkResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3">
         <parameters>
            <arbeidsforhold applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-12-05T14:41:08.679+01:00" opphavREF="eda00000-0000-0000-0000-001527873728" opprettelsestidspunkt="2018-07-03T21:35:20.946+02:00" opprettetAv="srvappserver" sistBekreftet="2018-12-05T14:29:56.000+01:00">
               <arbeidsforholdID>536200000000009692001</arbeidsforholdID>
               <arbeidsforholdIDnav>45526864</arbeidsforholdIDnav>
               <ansettelsesPeriode applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-07-03T21:35:20.946+02:00" fomBruksperiode="2018-07-03+02:00" opphavREF="eda00000-0000-0000-0000-001280397627">
                  <periode>
                     <fom>2018-06-06T00:00:00.000+02:00</fom>
                  </periode>
               </ansettelsesPeriode>
               <arbeidsforholdstype kodeRef="ordinaertArbeidsforhold">Ordinært arbeidsforhold</arbeidsforholdstype>
               <antallTimerForTimeloennet applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-10-19T09:45:47.917+02:00" opphavREF="eda00000-0000-0000-0000-001441005335">
                  <periode>
                     <fom>2018-09-01T00:00:00.000+02:00</fom>
                     <tom>2018-09-01T00:00:00.000+02:00</tom>
                  </periode>
                  <antallTimer>6.0</antallTimer>
                  <rapporteringsperiode>2018-10+02:00</rapporteringsperiode>
               </antallTimerForTimeloennet>
               <antallTimerForTimeloennet applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-11-02T15:42:18.776+01:00" opphavREF="eda00000-0000-0000-0000-001474433625">
                  <periode>
                     <fom>2018-10-01T00:00:00.000+02:00</fom>
                     <tom>2018-10-01T00:00:00.000+02:00</tom>
                  </periode>
                  <antallTimer>15.0</antallTimer>
                  <rapporteringsperiode>2018-11+01:00</rapporteringsperiode>
               </antallTimerForTimeloennet>
               <antallTimerForTimeloennet applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-12-05T14:41:08.679+01:00" opphavREF="eda00000-0000-0000-0000-001527873728">
                  <periode>
                     <fom>2018-11-01T00:00:00.000+01:00</fom>
                     <tom>2018-11-01T00:00:00.000+01:00</tom>
                  </periode>
                  <antallTimer>17.0</antallTimer>
                  <rapporteringsperiode>2018-12+01:00</rapporteringsperiode>
               </antallTimerForTimeloennet>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-11-02T15:42:18.776+01:00" fomBruksperiode="2018-11-02+01:00" fomGyldighetsperiode="2018-11-01T00:00:00.000+01:00" opphavREF="eda00000-0000-0000-0000-001474433625">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>100.0</stillingsprosent>
                  <sisteLoennsendringsdato>2018-09-01+02:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>37.5</beregnetAntallTimerPrUke>
                  <endringsdatoStillingsprosent>2018-06-06+02:00</endringsdatoStillingsprosent>
               </arbeidsavtale>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-11-02T15:42:18.769+01:00" fomBruksperiode="2018-11-02+01:00" fomGyldighetsperiode="2018-10-01T00:00:00.000+02:00" opphavREF="eda00000-0000-0000-0000-001474433625" tomGyldighetsperiode="2018-10-31T00:00:00.000+01:00">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>0.0</stillingsprosent>
                  <sisteLoennsendringsdato>2018-09-01+02:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>0.0</beregnetAntallTimerPrUke>
                  <endringsdatoStillingsprosent>2018-06-06+02:00</endringsdatoStillingsprosent>
               </arbeidsavtale>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-10-19T09:45:47.905+02:00" fomBruksperiode="2018-10-19+02:00" fomGyldighetsperiode="2018-07-01T00:00:00.000+02:00" opphavREF="eda00000-0000-0000-0000-001441005335" tomGyldighetsperiode="2018-09-30T00:00:00.000+02:00">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>0.0</stillingsprosent>
                  <sisteLoennsendringsdato>2018-06-06+02:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>0.0</beregnetAntallTimerPrUke>
                  <endringsdatoStillingsprosent>2018-06-06+02:00</endringsdatoStillingsprosent>
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
                  <orgnummer>984054564</orgnummer>
               </opplysningspliktig>
               <arbeidsforholdInnrapportertEtterAOrdningen>true</arbeidsforholdInnrapportertEtterAOrdningen>
            </arbeidsforhold>
         </parameters>
      </ns2:hentArbeidsforholdHistorikkResponse>
   </soap:Body>
</soap:Envelope>
""".trimIndent()

private val expectedJson = """
[
  {
    "permisjonOgPermittering": [],
    "endretAv": "Z991012",
    "antallTimerForTimeloennet": [],
    "arbeidsforholdInnrapportertEtterAOrdningen": true,
    "arbeidsforholdID": "1",
    "opplysningspliktig": {
      "orgnummer": "910970046"
    },
    "arbeidsavtale": [
      {
        "avtaltArbeidstimerPerUke": 40,
        "fomGyldighetsperiode": "2017-04-01T00:00:00.000+02:00",
        "endretAv": "Z991012",
        "arbeidstidsordning": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
          "kodeRef": "ikkeSkift",
          "value": "Ikke skift"
        },
        "fomBruksperiode": "2018-10-30+01:00",
        "opphavREF": "3",
        "avloenningstype": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Avloenningstyper",
          "kodeRef": "fast",
          "value": "FastlÃ¸nn"
        },
        "yrke": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Yrker",
          "kodeRef": "3431101",
          "value": "ADMINISTRASJONSSEKRETÃ\u0086R"
        },
        "endringstidspunkt": "2018-10-30T11:26:37.029+01:00",
        "beregnetAntallTimerPrUke": 40,
        "applikasjonsID": "AAREG",
        "stillingsprosent": 100
      },
      {
        "avtaltArbeidstimerPerUke": 37.5,
        "sisteLoennsendringsdato": "2018-05-01+02:00",
        "fomGyldighetsperiode": "2018-06-01T00:00:00.000+02:00",
        "endretAv": "srvappserver",
        "arbeidstidsordning": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
          "kodeRef": "ikkeSkift",
          "value": "Ikke skift"
        },
        "fomBruksperiode": "2018-07-03+02:00",
        "opphavREF": "eda00000-0000-0000-0000-001280332174",
        "yrke": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Yrker",
          "kodeRef": "9160118",
          "value": "GJENVINNINGSARBEIDER"
        },
        "endringstidspunkt": "2018-07-03T21:05:50.328+02:00",
        "beregnetAntallTimerPrUke": 9.375,
        "applikasjonsID": "EDAG",
        "stillingsprosent": 25
      }
    ],
    "opphavREF": "3",
    "endringstidspunkt": "2018-10-30T11:26:37.029+01:00",
    "arbeidsforholdIDnav": 45526756,
    "utenlandsopphold": [],
    "arbeidsgiver": {
      "orgnummer": "910831143",
      "navn": "Maxbo"
    },
    "arbeidsforholdstype": {
      "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidsforholdstyper",
      "kodeRef": "ordinaertArbeidsforhold",
      "value": "OrdinÃ¦rt arbeidsforhold"
    },
    "opprettelsestidspunkt": "2018-10-30T11:26:37.029+01:00",
    "arbeidstaker": {
      "ident": {
        "ident": "08088806280"
      }
    },
    "sistBekreftet": "2018-10-30T11:26:36.000+01:00",
    "ansettelsesPeriode": {
      "endretAv": "Z991012",
      "fomBruksperiode": "2018-10-30+01:00",
      "opphavREF": "3",
      "endringstidspunkt": "2018-10-30T11:26:37.029+01:00",
      "applikasjonsID": "AAREG",
      "periode": {
        "fom": "2017-04-01T00:00:00.000+02:00"
      }
    },
    "opprettetAv": "Z991012",
    "applikasjonsID": "AAREG"
  },
  {
    "permisjonOgPermittering": [],
    "endretAv": "Z991012",
    "antallTimerForTimeloennet": [],
    "arbeidsforholdInnrapportertEtterAOrdningen": true,
    "arbeidsforholdID": "147",
    "opplysningspliktig": {
      "orgnummer": "923609016"
    },
    "arbeidsavtale": [
      {
        "avtaltArbeidstimerPerUke": 40,
        "fomGyldighetsperiode": "2017-04-01T00:00:00.000+02:00",
        "endretAv": "Z991012",
        "arbeidstidsordning": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
          "kodeRef": "ikkeSkift",
          "value": "Ikke skift"
        },
        "fomBruksperiode": "2018-10-30+01:00",
        "opphavREF": "3",
        "avloenningstype": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Avloenningstyper",
          "kodeRef": "fast",
          "value": "FastlÃ¸nn"
        },
        "yrke": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Yrker",
          "kodeRef": "3431101",
          "value": "ADMINISTRASJONSSEKRETÃ\u0086R"
        },
        "endringstidspunkt": "2018-10-30T11:38:54.966+01:00",
        "beregnetAntallTimerPrUke": 40,
        "applikasjonsID": "AAREG",
        "stillingsprosent": 100
      },
      {
        "endretAv": "srvappserver",
        "opphavREF": "eda00000-0000-0000-0000-001474433625",
        "avloenningstype": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Avloenningstyper",
          "kodeRef": "time",
          "value": "<Fant ingen gyldig term for kode: time>"
        },
        "yrke": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Yrker",
          "kodeRef": "8323102",
          "value": "SJÃ\u0085FÃ\u0098R (LASTEBIL)"
        },
        "endringstidspunkt": "2018-11-02T15:42:18.776+01:00",
        "stillingsprosent": 100,
        "avtaltArbeidstimerPerUke": 37.5,
        "sisteLoennsendringsdato": "2018-09-01+02:00",
        "fomGyldighetsperiode": "2018-11-01T00:00:00.000+01:00",
        "arbeidstidsordning": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
          "kodeRef": "ikkeSkift",
          "value": "Ikke skift"
        },
        "endringsdatoStillingsprosent": "2018-06-06+02:00",
        "fomBruksperiode": "2018-11-02+01:00",
        "beregnetAntallTimerPrUke": 37.5,
        "applikasjonsID": "EDAG"
      },
      {
        "endretAv": "srvappserver",
        "opphavREF": "eda00000-0000-0000-0000-001474433625",
        "avloenningstype": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Avloenningstyper",
          "kodeRef": "time",
          "value": "<Fant ingen gyldig term for kode: time>"
        },
        "yrke": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Yrker",
          "kodeRef": "8323102",
          "value": "SJÃ\u0085FÃ\u0098R (LASTEBIL)"
        },
        "endringstidspunkt": "2018-11-02T15:42:18.769+01:00",
        "tomGyldighetsperiode": "2018-10-31T00:00:00.000+01:00",
        "stillingsprosent": 0,
        "avtaltArbeidstimerPerUke": 37.5,
        "sisteLoennsendringsdato": "2018-09-01+02:00",
        "fomGyldighetsperiode": "2018-10-01T00:00:00.000+02:00",
        "arbeidstidsordning": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
          "kodeRef": "ikkeSkift",
          "value": "Ikke skift"
        },
        "endringsdatoStillingsprosent": "2018-06-06+02:00",
        "fomBruksperiode": "2018-11-02+01:00",
        "beregnetAntallTimerPrUke": 0,
        "applikasjonsID": "EDAG"
      },
      {
        "endretAv": "srvappserver",
        "opphavREF": "eda00000-0000-0000-0000-001441005335",
        "avloenningstype": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Avloenningstyper",
          "kodeRef": "time",
          "value": "<Fant ingen gyldig term for kode: time>"
        },
        "yrke": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Yrker",
          "kodeRef": "8323102",
          "value": "SJÃ\u0085FÃ\u0098R (LASTEBIL)"
        },
        "endringstidspunkt": "2018-10-19T09:45:47.905+02:00",
        "tomGyldighetsperiode": "2018-09-30T00:00:00.000+02:00",
        "stillingsprosent": 0,
        "avtaltArbeidstimerPerUke": 37.5,
        "sisteLoennsendringsdato": "2018-06-06+02:00",
        "fomGyldighetsperiode": "2018-07-01T00:00:00.000+02:00",
        "arbeidstidsordning": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
          "kodeRef": "ikkeSkift",
          "value": "Ikke skift"
        },
        "endringsdatoStillingsprosent": "2018-06-06+02:00",
        "fomBruksperiode": "2018-10-19+02:00",
        "beregnetAntallTimerPrUke": 0,
        "applikasjonsID": "EDAG"
      }
    ],
    "opphavREF": "3",
    "endringstidspunkt": "2018-10-30T11:38:54.966+01:00",
    "arbeidsforholdIDnav": 45526864,
    "utenlandsopphold": [],
    "arbeidsgiver": {
      "orgnummer": "973861778",
      "navn": "Telenor"
    },
    "arbeidsforholdstype": {
      "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidsforholdstyper",
      "kodeRef": "ordinaertArbeidsforhold",
      "value": "OrdinÃ¦rt arbeidsforhold"
    },
    "opprettelsestidspunkt": "2018-10-30T11:38:54.966+01:00",
    "arbeidstaker": {
      "ident": {
        "ident": "08088806280"
      }
    },
    "sistBekreftet": "2018-10-30T11:38:54.000+01:00",
    "ansettelsesPeriode": {
      "endretAv": "Z991012",
      "fomBruksperiode": "2018-10-30+01:00",
      "opphavREF": "3",
      "endringstidspunkt": "2018-10-30T11:38:54.966+01:00",
      "applikasjonsID": "AAREG",
      "periode": {
        "fom": "2017-04-01T00:00:00.000+02:00"
      }
    },
    "opprettetAv": "Z991012",
    "applikasjonsID": "AAREG"
  }
]
""".trimIndent()
