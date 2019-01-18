package no.nav.helse.ws.arbeidsforhold

import com.github.tomakehurst.wiremock.client.WireMock
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.helse.*
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import org.json.JSONObject
import org.junit.jupiter.api.*
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

    @Test
    fun `that response is json`() {
        val fnr = "08088806280"
        val token = bootstrap.jwkStub.createTokenFor("srvpleiepengesokna")



        WireMock.stubFor(aktørregisterStub(fnr, "12346", fnr))

        WireMock.stubFor(finnArbeidsforholdPrArbeidstakerStub(fnr)
                .withSamlAssertion()
                .withCallId()
                .willReturn(WireMock.okXml(finnArbeidsforholdPrArbeidstaker_response)))


        withTestApplication({sparkel(bootstrap.env, bootstrap.jwkStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, "/api/ident?fnr=$fnr") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(200, response.status()?.value)
                val identResponse = JSONObject(response.content!!)
                handleRequest(HttpMethod.Get, "/api/arbeidsforhold?uuid=${identResponse.get("id")}") {
                    addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer $token")
                }.apply {
                    assertEquals(200, response.status()?.value)
                    assertJsonEquals(JSONObject(expectedJson), JSONObject(response.content))
                }
            }
        }
    }

    @Test
    fun `that an unknown uuid requests`() {
        val token = bootstrap.jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({sparkel(bootstrap.env, bootstrap.jwkStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsforhold?uuid=123-will-not-be-found") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(404, response.status()?.value)
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

private val expectedJson = """
{
  "arbeidsforhold": [
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
          "endretAv": "Z991012",
          "fomGyldighetsperiode": "2017-04-01T00:00:00.000+02:00",
          "arbeidstidsordning": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
            "kodeRef": "ikkeSkift",
            "value": "Ikke skift"
          },
          "fomBruksperiode": "2018-10-30+01:00",
          "opphavREF": "3",
          "yrke": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Yrker",
            "kodeRef": "3431101",
            "value": "ADMINISTRASJONSSEKRETÃ\u0086R"
          },
          "avloenningstype": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Avloenningstyper",
            "kodeRef": "fast",
            "value": "FastlÃ¸nn"
          },
          "endringstidspunkt": "2018-10-30T11:26:37.029+01:00",
          "beregnetAntallTimerPrUke": 40,
          "applikasjonsID": "AAREG",
          "stillingsprosent": 100
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
          "endretAv": "Z991012",
          "fomGyldighetsperiode": "2017-04-01T00:00:00.000+02:00",
          "arbeidstidsordning": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger",
            "kodeRef": "ikkeSkift",
            "value": "Ikke skift"
          },
          "fomBruksperiode": "2018-10-30+01:00",
          "opphavREF": "3",
          "yrke": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Yrker",
            "kodeRef": "3431101",
            "value": "ADMINISTRASJONSSEKRETÃ\u0086R"
          },
          "avloenningstype": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Avloenningstyper",
            "kodeRef": "fast",
            "value": "FastlÃ¸nn"
          },
          "endringstidspunkt": "2018-10-30T11:38:54.966+01:00",
          "beregnetAntallTimerPrUke": 40,
          "applikasjonsID": "AAREG",
          "stillingsprosent": 100
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
}
""".trimIndent()
