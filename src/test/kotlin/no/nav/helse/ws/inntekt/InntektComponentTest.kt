package no.nav.helse.ws.inntekt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.prometheus.client.CollectorRegistry
import no.nav.helse.Environment
import no.nav.helse.JwtStub
import no.nav.helse.assertJsonEquals
import no.nav.helse.http.aktør.aktørregisterStub
import no.nav.helse.sparkel
import no.nav.helse.sts.stsRestStub
import no.nav.helse.ws.samlAssertionResponse
import no.nav.helse.ws.stsStub
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test



class InntektComponentTest {

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
        WireMock.configureFor(server.port())
    }

    @AfterEach
    fun `clear prometheus registry`() {
        CollectorRegistry.defaultRegistry.clear()
    }

    @Test
    fun `that response is json`() {
        val jwtStub = JwtStub("test issuer")
        val token = jwtStub.createTokenFor("srvspinne")

        WireMock.stubFor(stsRestStub())
        WireMock.stubFor(aktørregisterStub("1831212532188", "1831212532188", "13119924167"))

        WireMock.stubFor(stsStub("stsUsername", "stsPassword")
                .willReturn(samlAssertionResponse("testusername", "theIssuer", "CN=B27 Issuing CA Intern, DC=preprod, DC=local",
                        "digestValue", "signatureValue", "certificateValue")))

        WireMock.stubFor(hentInntektListeBolkStub("13119924167", "2017-01+01:00", "2018-01+01:00")
                .withSamlAssertion("testusername", "theIssuer", "CN=B27 Issuing CA Intern, DC=preprod, DC=local",
                        "digestValue", "signatureValue", "certificateValue")
                .withCallId("Sett inn call id her")
                .willReturn(WireMock.okXml(hentInntektListeBolk_response)))

        val env = Environment(mapOf(
                "SECURITY_TOKEN_SERVICE_REST_URL" to server.baseUrl(),
                "SECURITY_TOKEN_SERVICE_URL" to server.baseUrl().plus("/sts"),
                "SECURITY_TOKEN_SERVICE_USERNAME" to "stsUsername",
                "SECURITY_TOKEN_SERVICE_PASSWORD" to "stsPassword",
                "AKTORREGISTER_URL" to server.baseUrl(),
                "INNTEKT_ENDPOINTURL" to server.baseUrl().plus("/inntekt"),
                "JWT_ISSUER" to "test issuer",
                "ALLOW_INSECURE_SOAP_REQUESTS" to "true"
        ))

        withTestApplication({sparkel(env, jwtStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, "/api/inntekt/1831212532188?fom=2017-01&tom=2018-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                Assertions.assertEquals(200, response.status()?.value)
                assertJsonEquals(JSONObject(expectedJson), JSONObject(response.content))
            }
        }
    }
}

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

private val expectedJson = """
{
  "sikkerhetsavvikListe": [],
  "arbeidsInntektIdentListe": [
    {
      "ident": {
        "personIdent": "13119924167"
      },
      "arbeidsInntektMaaned": [
        {
          "arbeidsInntektInformasjon": {
            "inntektListe": [
              {
                "utloeserArbeidsgiveravgift": true,
                "inntektsmottaker": {
                  "personIdent": "13119924167"
                },
                "opplysningspliktig": {
                  "orgnummer": "973861778"
                },
                "informasjonsstatus": {
                  "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Informasjonsstatuser",
                  "value": "InngaarAlltid"
                },
                "virksomhet": {
                  "orgnummer": "973861778"
                },
                "beloep": 40000,
                "levereringstidspunkt": "2018-12-05T09:50:10.777+01:00",
                "inntektsstatus": {
                  "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Inntektsstatuser",
                  "value": "LoependeInnrapportert"
                },
                "inngaarIGrunnlagForTrekk": true,
                "inntektsperiodetype": {
                  "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Inntektsperiodetyper",
                  "value": "Maaned"
                },
                "fordel": {
                  "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Fordel",
                  "value": "kontantytelse"
                },
                "beskrivelse": {
                  "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Loennsbeskrivelse",
                  "value": "fastloenn"
                },
                "inntektskilde": {
                  "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/InntektsInformasjonsopphav",
                  "value": "A-ordningen"
                },
                "utbetaltIPeriode": "2017-12"
              }
            ],
            "arbeidsforholdListe": [],
            "forskuddstrekkListe": [],
            "fradragListe": []
          },
          "aarMaaned": "2017-12+01:00",
          "avvikListe": []
        },
        {
          "arbeidsInntektInformasjon": {
            "inntektListe": [
              {
                "utloeserArbeidsgiveravgift": true,
                "inntektsmottaker": {
                  "personIdent": "13119924167"
                },
                "opplysningspliktig": {
                  "orgnummer": "973861778"
                },
                "informasjonsstatus": {
                  "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Informasjonsstatuser",
                  "value": "InngaarAlltid"
                },
                "virksomhet": {
                  "orgnummer": "973861778"
                },
                "beloep": 40000,
                "levereringstidspunkt": "2018-12-05T09:50:10.777+01:00",
                "inntektsstatus": {
                  "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Inntektsstatuser",
                  "value": "LoependeInnrapportert"
                },
                "inngaarIGrunnlagForTrekk": true,
                "inntektsperiodetype": {
                  "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Inntektsperiodetyper",
                  "value": "Maaned"
                },
                "fordel": {
                  "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Fordel",
                  "value": "kontantytelse"
                },
                "beskrivelse": {
                  "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Loennsbeskrivelse",
                  "value": "fastloenn"
                },
                "inntektskilde": {
                  "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/InntektsInformasjonsopphav",
                  "value": "A-ordningen"
                },
                "utbetaltIPeriode": "2018-01"
              }
            ],
            "arbeidsforholdListe": [],
            "forskuddstrekkListe": [],
            "fradragListe": []
          },
          "aarMaaned": "2018-01+01:00",
          "avvikListe": []
        }
      ]
    }
  ]
}
""".trimIndent()
