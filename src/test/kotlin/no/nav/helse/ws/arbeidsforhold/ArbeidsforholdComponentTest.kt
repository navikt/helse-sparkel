package no.nav.helse.ws.arbeidsforhold

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.prometheus.client.CollectorRegistry
import no.nav.helse.aktørregisterStub
import no.nav.helse.assertJsonEquals
import no.nav.helse.bootstrapComponentTest
import no.nav.helse.sparkel
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import org.json.JSONArray
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

        WireMock.stubFor(hentArbeidsforholdHistorikkStub("38009429")
                .withSamlAssertion()
                .withCallId()
                .inScenario("aareg")
                .whenScenarioStateIs("arbeidsforhold_hentet")
                .willSetStateTo("første_arbeidsforholdhistorikk_hentet")
                .willReturn(WireMock.okXml(hentArbeidsforholdHistorikk_response1)))

        WireMock.stubFor(hentArbeidsforholdHistorikkStub("44669403")
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
                  <orgnummer>984054564</orgnummer>
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
      </ns2:finnArbeidsforholdPrArbeidstakerResponse>
   </soap:Body>
</soap:Envelope>
""".trimIndent()

private val hentArbeidsforholdHistorikk_response1 = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:hentArbeidsforholdHistorikkResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3">
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
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-01-05T13:31:11.170+01:00" fomBruksperiode="2018-01-05+01:00" fomGyldighetsperiode="2017-11-01T00:00:00.000+01:00" opphavREF="eda00000-0000-0000-0000-000972417562" tomGyldighetsperiode="2017-11-30T00:00:00.000+01:00">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>100.0</stillingsprosent>
                  <sisteLoennsendringsdato>2017-11-01+01:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>37.5</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2017-12-05T15:21:36.512+01:00" fomBruksperiode="2017-12-05+01:00" fomGyldighetsperiode="2017-10-01T00:00:00.000+02:00" opphavREF="eda00000-0000-0000-0000-000913989552" tomGyldighetsperiode="2017-10-31T00:00:00.000+01:00">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>100.0</stillingsprosent>
                  <sisteLoennsendringsdato>2017-10-01+02:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>37.5</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2017-11-06T13:24:37.221+01:00" fomBruksperiode="2017-11-06+01:00" fomGyldighetsperiode="2017-09-01T00:00:00.000+02:00" opphavREF="eda00000-0000-0000-0000-000858962424" tomGyldighetsperiode="2017-09-30T00:00:00.000+02:00">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>100.0</stillingsprosent>
                  <sisteLoennsendringsdato>2017-09-01+02:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>37.5</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2017-10-05T19:49:46.263+02:00" fomBruksperiode="2017-10-05+02:00" fomGyldighetsperiode="2017-08-01T00:00:00.000+02:00" opphavREF="eda00000-0000-0000-0000-000809027622" tomGyldighetsperiode="2017-08-31T00:00:00.000+02:00">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>100.0</stillingsprosent>
                  <sisteLoennsendringsdato>2017-08-01+02:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>37.5</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2017-09-05T16:02:15.907+02:00" fomBruksperiode="2017-09-05+02:00" fomGyldighetsperiode="2017-01-01T00:00:00.000+01:00" opphavREF="eda00000-0000-0000-0000-000754811568" tomGyldighetsperiode="2017-07-31T00:00:00.000+02:00">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>100.0</stillingsprosent>
                  <sisteLoennsendringsdato>2015-09-19+02:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>37.5</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2017-02-10T08:16:58.456+01:00" fomBruksperiode="2017-02-10+01:00" fomGyldighetsperiode="2015-12-01T00:00:00.000+01:00" opphavREF="eda00000-0000-0000-0000-000437670475" tomGyldighetsperiode="2016-12-31T00:00:00.000+01:00">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>0.0</stillingsprosent>
                  <sisteLoennsendringsdato>2015-09-19+02:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>0.0</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2016-01-05T20:37:17.806+01:00" fomBruksperiode="2016-01-05+01:00" fomGyldighetsperiode="2015-11-01T00:00:00.000+01:00" opphavREF="84a6b1de-484f-492d-a1c4-969827c2a4e0" tomGyldighetsperiode="2015-11-30T00:00:00.000+01:00">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>0.0</stillingsprosent>
                  <sisteLoennsendringsdato>2015-11-01+01:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>0.0</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2015-12-04T15:23:27.334+01:00" fomBruksperiode="2015-12-04+01:00" fomGyldighetsperiode="2015-10-01T00:00:00.000+02:00" opphavREF="092da794-e540-48af-85de-05b66c330138" tomGyldighetsperiode="2015-10-31T00:00:00.000+01:00">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>0.0</stillingsprosent>
                  <sisteLoennsendringsdato>2015-10-01+02:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>0.0</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2015-11-05T12:48:16.406+01:00" fomBruksperiode="2015-11-05+01:00" fomGyldighetsperiode="2015-09-01T00:00:00.000+02:00" opphavREF="35e5241a-425e-44b2-b32e-50c17bac570e" tomGyldighetsperiode="2015-09-30T00:00:00.000+02:00">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>0.0</stillingsprosent>
                  <sisteLoennsendringsdato>2015-09-01+02:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>0.0</beregnetAntallTimerPrUke>
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

private val hentArbeidsforholdHistorikk_response2 = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:hentArbeidsforholdHistorikkResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3">
         <parameters>
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
    "endretAv": "srvappserver",
    "antallTimerForTimeloennet": [
      {
        "antallTimer": 2,
        "endretAv": "srvappserver",
        "rapporteringsperiode": "2015-09+02:00",
        "opphavREF": "06c8a5b7-2d59-44fd-9f98-79cc4fc44a65",
        "endringstidspunkt": "2015-10-05T14:00:16.809+02:00",
        "applikasjonsID": "EDAG",
        "periode": {
          "tom": "2015-09-30T00:00:00.000+02:00",
          "fom": "2015-09-01T00:00:00.000+02:00"
        }
      },
      {
        "antallTimer": 15.8,
        "endretAv": "srvappserver",
        "rapporteringsperiode": "2015-10+02:00",
        "opphavREF": "35e5241a-425e-44b2-b32e-50c17bac570e",
        "endringstidspunkt": "2015-11-05T12:48:16.406+01:00",
        "applikasjonsID": "EDAG",
        "periode": {
          "tom": "2015-10-31T00:00:00.000+01:00",
          "fom": "2015-10-01T00:00:00.000+02:00"
        }
      },
      {
        "antallTimer": 8,
        "endretAv": "srvappserver",
        "rapporteringsperiode": "2015-11+01:00",
        "opphavREF": "092da794-e540-48af-85de-05b66c330138",
        "endringstidspunkt": "2015-12-04T15:23:27.334+01:00",
        "applikasjonsID": "EDAG",
        "periode": {
          "tom": "2015-11-30T00:00:00.000+01:00",
          "fom": "2015-11-01T00:00:00.000+01:00"
        }
      }
    ],
    "arbeidsforholdInnrapportertEtterAOrdningen": true,
    "arbeidsforholdID": "00000009692000000001",
    "opplysningspliktig": {
      "orgnummer": "984054564"
    },
    "arbeidsavtale": [
      {
        "endretAv": "srvappserver",
        "opphavREF": "eda00000-0000-0000-0000-000972417562",
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
        "endringstidspunkt": "2018-01-05T13:31:11.177+01:00",
        "stillingsprosent": 100,
        "avtaltArbeidstimerPerUke": 37.5,
        "sisteLoennsendringsdato": "2017-12-01+01:00",
        "fomGyldighetsperiode": "2017-12-01T00:00:00.000+01:00",
        "arbeidstidsordning": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
          "kodeRef": "ikkeSkift",
          "value": "Ikke skift"
        },
        "fomBruksperiode": "2018-01-05+01:00",
        "beregnetAntallTimerPrUke": 37.5,
        "applikasjonsID": "EDAG"
      },
      {
        "endretAv": "srvappserver",
        "opphavREF": "eda00000-0000-0000-0000-000972417562",
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
        "tomGyldighetsperiode": "2017-11-30T00:00:00.000+01:00",
        "endringstidspunkt": "2018-01-05T13:31:11.170+01:00",
        "stillingsprosent": 100,
        "avtaltArbeidstimerPerUke": 37.5,
        "sisteLoennsendringsdato": "2017-11-01+01:00",
        "fomGyldighetsperiode": "2017-11-01T00:00:00.000+01:00",
        "arbeidstidsordning": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
          "kodeRef": "ikkeSkift",
          "value": "Ikke skift"
        },
        "fomBruksperiode": "2018-01-05+01:00",
        "beregnetAntallTimerPrUke": 37.5,
        "applikasjonsID": "EDAG"
      },
      {
        "endretAv": "srvappserver",
        "opphavREF": "eda00000-0000-0000-0000-000913989552",
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
        "tomGyldighetsperiode": "2017-10-31T00:00:00.000+01:00",
        "endringstidspunkt": "2017-12-05T15:21:36.512+01:00",
        "stillingsprosent": 100,
        "avtaltArbeidstimerPerUke": 37.5,
        "sisteLoennsendringsdato": "2017-10-01+02:00",
        "fomGyldighetsperiode": "2017-10-01T00:00:00.000+02:00",
        "arbeidstidsordning": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
          "kodeRef": "ikkeSkift",
          "value": "Ikke skift"
        },
        "fomBruksperiode": "2017-12-05+01:00",
        "beregnetAntallTimerPrUke": 37.5,
        "applikasjonsID": "EDAG"
      },
      {
        "endretAv": "srvappserver",
        "opphavREF": "eda00000-0000-0000-0000-000858962424",
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
        "tomGyldighetsperiode": "2017-09-30T00:00:00.000+02:00",
        "endringstidspunkt": "2017-11-06T13:24:37.221+01:00",
        "stillingsprosent": 100,
        "avtaltArbeidstimerPerUke": 37.5,
        "sisteLoennsendringsdato": "2017-09-01+02:00",
        "fomGyldighetsperiode": "2017-09-01T00:00:00.000+02:00",
        "arbeidstidsordning": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
          "kodeRef": "ikkeSkift",
          "value": "Ikke skift"
        },
        "fomBruksperiode": "2017-11-06+01:00",
        "beregnetAntallTimerPrUke": 37.5,
        "applikasjonsID": "EDAG"
      },
      {
        "endretAv": "srvappserver",
        "opphavREF": "eda00000-0000-0000-0000-000809027622",
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
        "tomGyldighetsperiode": "2017-08-31T00:00:00.000+02:00",
        "endringstidspunkt": "2017-10-05T19:49:46.263+02:00",
        "stillingsprosent": 100,
        "avtaltArbeidstimerPerUke": 37.5,
        "sisteLoennsendringsdato": "2017-08-01+02:00",
        "fomGyldighetsperiode": "2017-08-01T00:00:00.000+02:00",
        "arbeidstidsordning": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
          "kodeRef": "ikkeSkift",
          "value": "Ikke skift"
        },
        "fomBruksperiode": "2017-10-05+02:00",
        "beregnetAntallTimerPrUke": 37.5,
        "applikasjonsID": "EDAG"
      },
      {
        "endretAv": "srvappserver",
        "opphavREF": "eda00000-0000-0000-0000-000754811568",
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
        "tomGyldighetsperiode": "2017-07-31T00:00:00.000+02:00",
        "endringstidspunkt": "2017-09-05T16:02:15.907+02:00",
        "stillingsprosent": 100,
        "avtaltArbeidstimerPerUke": 37.5,
        "sisteLoennsendringsdato": "2015-09-19+02:00",
        "fomGyldighetsperiode": "2017-01-01T00:00:00.000+01:00",
        "arbeidstidsordning": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
          "kodeRef": "ikkeSkift",
          "value": "Ikke skift"
        },
        "fomBruksperiode": "2017-09-05+02:00",
        "beregnetAntallTimerPrUke": 37.5,
        "applikasjonsID": "EDAG"
      }
    ],
    "opphavREF": "eda00000-0000-0000-0000-000972417562",
    "endringstidspunkt": "2018-01-05T13:31:11.214+01:00",
    "arbeidsforholdIDnav": 38009429,
    "utenlandsopphold": [],
    "arbeidsgiver": {
      "orgnummer": "913548221"
    },
    "arbeidsforholdstype": {
      "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidsforholdstyper",
      "kodeRef": "ordinaertArbeidsforhold",
      "value": "OrdinÃ¦rt arbeidsforhold"
    },
    "opprettelsestidspunkt": "2015-10-05T14:00:16.809+02:00",
    "arbeidstaker": {
      "ident": {
        "ident": "07067625948"
      }
    },
    "sistBekreftet": "2018-01-05T13:29:41.000+01:00",
    "ansettelsesPeriode": {
      "endretAv": "srvappserver",
      "fomBruksperiode": "2018-01-05+01:00",
      "opphavREF": "eda00000-0000-0000-0000-000972417562",
      "endringstidspunkt": "2018-01-05T13:31:11.177+01:00",
      "applikasjonsID": "EDAG",
      "periode": {
        "tom": "2017-11-30T00:00:00.000+01:00",
        "fom": "2015-09-19T00:00:00.000+02:00"
      }
    },
    "opprettetAv": "srvappserver",
    "applikasjonsID": "EDAG"
  },
  {
    "permisjonOgPermittering": [],
    "endretAv": "srvappserver",
    "antallTimerForTimeloennet": [],
    "arbeidsforholdInnrapportertEtterAOrdningen": true,
    "arbeidsforholdID": "00000009692000000005",
    "opplysningspliktig": {
      "orgnummer": "984054564"
    },
    "arbeidsavtale": [
      {
        "endretAv": "srvappserver",
        "opphavREF": "eda00000-0000-0000-0000-001041599767",
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
        "endringstidspunkt": "2018-02-07T11:44:40.548+01:00",
        "stillingsprosent": 0,
        "avtaltArbeidstimerPerUke": 37.5,
        "sisteLoennsendringsdato": "2015-09-19+02:00",
        "fomGyldighetsperiode": "2018-01-01T00:00:00.000+01:00",
        "arbeidstidsordning": {
          "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
          "kodeRef": "ikkeSkift",
          "value": "Ikke skift"
        },
        "fomBruksperiode": "2018-02-07+01:00",
        "beregnetAntallTimerPrUke": 0,
        "applikasjonsID": "EDAG"
      }
    ],
    "opphavREF": "eda00000-0000-0000-0000-001190431060",
    "endringstidspunkt": "2018-05-08T11:24:25.569+02:00",
    "arbeidsforholdIDnav": 44669403,
    "utenlandsopphold": [],
    "arbeidsgiver": {
      "orgnummer": "913548221"
    },
    "arbeidsforholdstype": {
      "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidsforholdstyper",
      "kodeRef": "ordinaertArbeidsforhold",
      "value": "OrdinÃ¦rt arbeidsforhold"
    },
    "opprettelsestidspunkt": "2018-02-07T11:44:40.548+01:00",
    "arbeidstaker": {
      "ident": {
        "ident": "07067625948"
      }
    },
    "sistBekreftet": "2018-05-08T11:13:39.000+02:00",
    "ansettelsesPeriode": {
      "endretAv": "srvappserver",
      "fomBruksperiode": "2018-02-07+01:00",
      "opphavREF": "eda00000-0000-0000-0000-001041599767",
      "endringstidspunkt": "2018-02-07T11:44:40.548+01:00",
      "applikasjonsID": "EDAG",
      "periode": {
        "tom": "2017-10-31T00:00:00.000+01:00",
        "fom": "2015-09-19T00:00:00.000+02:00"
      }
    },
    "opprettetAv": "srvappserver",
    "applikasjonsID": "EDAG"
  }
]
""".trimIndent()
