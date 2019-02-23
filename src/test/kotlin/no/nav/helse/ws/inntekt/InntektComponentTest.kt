package no.nav.helse.ws.inntekt

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.JwtStub
import no.nav.helse.assertJsonEquals
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.http.aktør.AktørregisterClient
import no.nav.helse.http.aktør.AktørregisterService
import no.nav.helse.mockedSparkel
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektIdent
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektInformasjon
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektMaaned
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Fordel
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Informasjonsstatuser
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.InntektsInformasjonsopphav
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntektsperiodetype
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntektsstatuser
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Loennsbeskrivelse
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Loennsinntekt
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.PersonIdent
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class InntektComponentTest {

    @Test
    fun `skal svare med liste av inntekter`() {
        val inntektV3 = mockk<InntektV3>()
        val aktørregisterClient = mockk<AktørregisterClient>()

        val aktørId = AktørId("11987654321")
        val fnr = Fødselsnummer("12345678911")
        val fom = YearMonth.parse("2019-01")

        val expected = HentInntektListeBolkResponse().apply {
            with (arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    ident = PersonIdent().apply {
                        personIdent = fnr.value
                    }
                    with (arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            aarMaaned = fom.toXmlGregorianCalendar()
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with (inntektListe) {
                                    add(Loennsinntekt().apply {
                                        beloep = BigDecimal.valueOf(2500)
                                        fordel = Fordel().apply {
                                            value = "kontantytelse"
                                        }
                                        inntektskilde = InntektsInformasjonsopphav().apply {
                                            value = "A-ordningen"
                                        }
                                        inntektsperiodetype = Inntektsperiodetype().apply {
                                            value = "Maaned"
                                        }
                                        inntektsstatus = Inntektsstatuser().apply {
                                            value = "LoependeInnrapportert"
                                        }
                                        levereringstidspunkt = fom.toXmlGregorianCalendar()
                                        utbetaltIPeriode = fom.toXmlGregorianCalendar()
                                        opplysningspliktig = Organisasjon().apply {
                                            orgnummer = "11223344"
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "11223344"
                                        }
                                        inntektsmottaker = PersonIdent().apply {
                                            personIdent = fnr.value
                                        }
                                        isInngaarIGrunnlagForTrekk = true
                                        isUtloeserArbeidsgiveravgift = true
                                        informasjonsstatus = Informasjonsstatuser().apply {
                                            value = "InngaarAlltid"
                                        }
                                        beskrivelse = Loennsbeskrivelse().apply {
                                            value = "fastloenn"
                                        }
                                    })
                                }
                            }
                        })
                    }
                })
            }
        }

        every {
            inntektV3.hentInntektListeBolk(any())
        } returns expected

        every {
            aktørregisterClient.gjeldendeNorskIdent(aktørId.aktor)
        } returns fnr.value

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                inntektService = InntektService(
                        inntektClient = InntektClient(inntektV3),
                        aktørregisterService = AktørregisterService(aktørregisterClient)
                ))}) {
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}?fom=2019-01&tom=2019-02") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.OK.value, response.status()?.value)
                assertJsonEquals(JSONObject(expectedJson), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `skal svare med feil ved feil`() {
        val inntektV3 = mockk<InntektV3>()
        val aktørregisterClient = mockk<AktørregisterClient>()

        val aktørId = AktørId("11987654321")
        val fnr = Fødselsnummer("12345678911")

        every {
            inntektV3.hentInntektListeBolk(any())
        } throws (Exception("SOAP fault"))

        every {
            aktørregisterClient.gjeldendeNorskIdent(aktørId.aktor)
        } returns fnr.value

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                inntektService = InntektService(
                        inntektClient = InntektClient(inntektV3),
                        aktørregisterService = AktørregisterService(aktørregisterClient)
                ))}) {
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}?fom=2019-01&tom=2019-02") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError.value, response.status()?.value)
                assertJsonEquals(JSONObject(expectedJson_fault), JSONObject(response.content))
            }
        }
    }
}

private val expectedJson = """
{
  "sikkerhetsavvikListe": [],
  "arbeidsInntektIdentListe": [
    {
      "ident": {
        "personIdent": "12345678911"
      },
      "arbeidsInntektMaaned": [
        {
          "arbeidsInntektInformasjon": {
            "inntektListe": [
              {
                "utloeserArbeidsgiveravgift": true,
                "inntektsmottaker": {
                  "personIdent": "12345678911"
                },
                "opplysningspliktig": {
                  "orgnummer": "11223344"
                },
                "informasjonsstatus": {
                  "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Informasjonsstatuser",
                  "value": "InngaarAlltid"
                },
                "virksomhet": {
                  "orgnummer": "11223344"
                },
                "beloep": 2500,
                "levereringstidspunkt": "2019-01-01T00:00:00.000Z",
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
                "utbetaltIPeriode": "2019-01-01T00:00:00.000Z"
              }
            ],
            "arbeidsforholdListe": [],
            "forskuddstrekkListe": [],
            "fradragListe": []
          },
          "aarMaaned": "2019-01-01T00:00:00.000Z",
          "avvikListe": []
        }
      ]
    }
  ]
}
""".trimIndent()

private val expectedJson_fault = """
{
    "exception":"java.lang.Exception: SOAP fault",
    "feilmelding":"SOAP fault"
}
""".trimIndent()
