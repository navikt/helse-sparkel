package no.nav.helse.domene.aiy

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
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.arbeid.ArbeidsforholdService
import no.nav.helse.domene.inntekt.InntektService
import no.nav.helse.domene.organisasjon.OrganisasjonService
import no.nav.helse.mockedSparkel
import no.nav.helse.oppslag.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.oppslag.inntekt.InntektClient
import no.nav.helse.oppslag.organisasjon.OrganisasjonClient
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.*
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Yrker
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkResponse
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Periode
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Virksomhet
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonResponse
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals

class ArbeidInntektYtelseComponentTest {

    @Test
    fun `feil returneres når fom ikke er satt`() {
        val aktørId = AktørId("1831212532188")

        val expected = """
            {
                "feilmelding": "you need to supply query parameter fom and tom"
            }
        """.trimIndent()

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider()
        )}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsforhold/${aktørId.aktor}/inntekter") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `feil returneres når tom ikke er satt`() {
        val aktørId = AktørId("1831212532188")

        val expected = """
            {
                "feilmelding": "you need to supply query parameter fom and tom"
            }
        """.trimIndent()

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider()
        )}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsforhold/${aktørId.aktor}/inntekter?fom=2019-01-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `feil returneres når fom er ugyldig`() {
        val aktørId = AktørId("1831212532188")

        val expected = """
            {
                "feilmelding": "fom must be specified as yyyy-mm-dd"
            }
        """.trimIndent()

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider()
        )}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsforhold/${aktørId.aktor}/inntekter?fom=foo&tom=2019-01-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `feil returneres når tom er ugyldig`() {
        val aktørId = AktørId("1831212532188")

        val expected = """
            {
                "feilmelding": "tom must be specified as yyyy-mm-dd"
            }
        """.trimIndent()

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider()
        )}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsforhold/${aktørId.aktor}/inntekter?fom=2019-01-01&tom=foo") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `en liste over arbeidsforhold og inntekter skal returneres`() {
        val arbeidsforholdV3 = mockk<ArbeidsforholdV3>()
        val inntektV3 = mockk<InntektV3>()
        val organisasjonV5 = mockk<OrganisasjonV5>()

        val aktørId = AktørId("1831212532188")

        val virksomhet = "889640782"

        every {
            arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(match {
                it.ident.ident == aktørId.aktor
            })
        } returns FinnArbeidsforholdPrArbeidstakerResponse().apply {
            with (arbeidsforhold) {
                add(Arbeidsforhold().apply {
                    arbeidsgiver = Organisasjon().apply {
                        orgnummer = virksomhet
                    }
                    arbeidsforholdIDnav = 1234L
                    ansettelsesPeriode = AnsettelsesPeriode().apply {
                        periode = Gyldighetsperiode().apply {
                            this.fom = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
                        }
                    }
                    with (arbeidsavtale) {
                        add(Arbeidsavtale().apply {
                            fomGyldighetsperiode = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
                            yrke = Yrker().apply {
                                value = "Butikkmedarbeider"
                            }
                            stillingsprosent = BigDecimal.valueOf(100)
                        })
                    }
                    with(permisjonOgPermittering) {
                        add(PermisjonOgPermittering().apply {
                            permisjonsPeriode = Gyldighetsperiode().apply {
                                this.fom = LocalDate.parse("2019-02-01").toXmlGregorianCalendar()
                                this.tom = LocalDate.parse("2019-02-28").toXmlGregorianCalendar()
                                permisjonsprosent = BigDecimal.valueOf(100)
                                permisjonOgPermittering = PermisjonsOgPermitteringsBeskrivelse().apply {
                                    value = "velferdspermisjon"
                                }
                            }
                        })
                    }
                })
            }
        }

        every {
            arbeidsforholdV3.hentArbeidsforholdHistorikk(match { request ->
                request.arbeidsforholdId == 1234L
            })
        } returns HentArbeidsforholdHistorikkResponse().apply {
            arbeidsforhold = Arbeidsforhold().apply {
                with(arbeidsavtale) {
                    add(Arbeidsavtale().apply {
                        fomGyldighetsperiode = LocalDate.parse("2019-02-01").toXmlGregorianCalendar()
                        yrke = Yrker().apply {
                            value = "Butikkmedarbeider"
                        }
                        stillingsprosent = BigDecimal.valueOf(100)
                    })
                    add(Arbeidsavtale().apply {
                        fomGyldighetsperiode = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
                        tomGyldighetsperiode = LocalDate.parse("2019-01-31").toXmlGregorianCalendar()
                        yrke = Yrker().apply {
                            value = "Butikkmedarbeider"
                        }
                        stillingsprosent = BigDecimal.valueOf(50)
                    })
                }
            }
        }

        val treInntekter = listeMedTreInntekter(aktørId, YearMonth.of(2017, 1), virksomhet)

        every {
            inntektV3.hentInntektListeBolk(match {
                it.identListe.size == 1 && (it.identListe[0] as AktoerId).aktoerId == aktørId.aktor
            })
        } returns treInntekter

        every {
            organisasjonV5.hentOrganisasjon(match {
                it.orgnummer == virksomhet
            })
        } returns HentOrganisasjonResponse().apply {
            organisasjon = Virksomhet().apply {
                orgnummer = virksomhet
                navn = UstrukturertNavn().apply {
                    with (navnelinje) {
                        add("NAV")
                    }
                }
            }
        }

        val organisasjonService = OrganisasjonService(
                organisasjonsClient = OrganisasjonClient(organisasjonV5)
        )

        val inntektClient = InntektClient(inntektV3)

        val arbeidsforholdService = ArbeidsforholdService(
                arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3),
                inntektClient = inntektClient
        )
        val inntektService = InntektService(
                inntektClient = inntektClient
        )

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                arbeidInntektYtelseService = ArbeidInntektYtelseService(
                        arbeidsforholdService = arbeidsforholdService,
                        inntektService = inntektService,
                        organisasjonService = organisasjonService
                ))}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsforhold/${aktørId.aktor}/inntekter?fom=2017-01-01&tom=2019-03-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertJsonEquals(JSONObject(expectedJson_inntekterMedArbeidsforhold), JSONObject(response.content))
            }
        }
    }

    private fun inntektUtenOpptjeningsperiode(aktørId: AktørId,
                                              virksomhet: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon,
                                              periode: YearMonth,
                                              beløp: Long) =
            Loennsinntekt().apply {
                beloep = BigDecimal.valueOf(beløp)
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
                levereringstidspunkt = periode.toXmlGregorianCalendar()
                utbetaltIPeriode = periode.toXmlGregorianCalendar()
                opplysningspliktig = virksomhet
                this.virksomhet = virksomhet
                inntektsmottaker = AktoerId().apply {
                    aktoerId = aktørId.aktor
                }
                isInngaarIGrunnlagForTrekk = true
                isUtloeserArbeidsgiveravgift = true
                informasjonsstatus = Informasjonsstatuser().apply {
                    value = "InngaarAlltid"
                }
                beskrivelse = Loennsbeskrivelse().apply {
                    value = "fastloenn"
                }
            }

    private fun inntektMedOpptjeningsperiode(aktørId: AktørId,
                                             virksomhet: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon,
                                             periode: YearMonth,
                                             opptjeningsperiodeFom: LocalDate,
                                             opptjeningsperiodeTom: LocalDate,
                                             beløp: Long) =
            inntektUtenOpptjeningsperiode(aktørId, virksomhet, periode, beløp).apply {
                opptjeningsperiode = Periode().apply {
                    startDato = opptjeningsperiodeFom.toXmlGregorianCalendar()
                    sluttDato = opptjeningsperiodeTom.toXmlGregorianCalendar()
                }
            }
    private fun listeMedTreInntekter(aktørId: AktørId, fom: YearMonth, virksomhet: String) = HentInntektListeBolkResponse().apply {
        with (arbeidsInntektIdentListe) {
            add(ArbeidsInntektIdent().apply {
                ident = AktoerId().apply {
                    aktoerId = aktørId.aktor
                }
                with (arbeidsInntektMaaned) {
                    add(ArbeidsInntektMaaned().apply {
                        aarMaaned = fom.toXmlGregorianCalendar()
                        arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                            with (inntektListe) {
                                add(inntektMedOpptjeningsperiode(
                                        aktørId = aktørId,
                                        virksomhet = no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon().apply {
                                            orgnummer = virksomhet
                                        },
                                        periode = YearMonth.of(2019, 1),
                                        opptjeningsperiodeFom = LocalDate.parse("2019-01-01"),
                                        opptjeningsperiodeTom = LocalDate.parse("2019-01-31"),
                                        beløp = 2500
                                ))
                                add(inntektUtenOpptjeningsperiode(
                                        aktørId = aktørId,
                                        virksomhet = no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon().apply {
                                            orgnummer = virksomhet
                                        },
                                        periode = YearMonth.of(2019, 2),
                                        beløp = 3500
                                ))
                                add(inntektUtenOpptjeningsperiode(
                                        aktørId = aktørId,
                                        virksomhet = no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon().apply {
                                            orgnummer = virksomhet
                                        },
                                        periode = YearMonth.of(2019, 2),
                                        beløp = 2500
                                ))
                                add(inntektMedOpptjeningsperiode(
                                        aktørId = aktørId,
                                        virksomhet = no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon().apply {
                                            orgnummer = virksomhet
                                        },
                                        periode = YearMonth.of(2019, 3),
                                        opptjeningsperiodeFom = LocalDate.parse("2019-03-01"),
                                        opptjeningsperiodeTom = LocalDate.parse("2019-03-31"),
                                        beløp = 2500
                                ))
                                add(inntektUtenOpptjeningsperiode(
                                        aktørId = aktørId,
                                        virksomhet = no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon().apply {
                                            orgnummer = virksomhet
                                        },
                                        periode = YearMonth.of(2019, 3),
                                        beløp = -500
                                ))
                            }
                        }
                    })
                }
            })
        }
    }
}

private val expectedJson_inntekterMedArbeidsforhold = """
{
  "inntekter": [
    {
      "muligeArbeidsforhold": [
        {
          "permisjon": [
            {
              "arsak": "velferdspermisjon",
              "tom": "2019-02-28",
              "fom": "2019-02-01",
              "permisjonsprosent": 100
            }
          ],
          "arbeidsgiver": {
            "identifikator": "889640782",
            "type": "Organisasjon"
          },
          "startdato": "2019-01-01",
          "arbeidsavtaler": [
            {
              "fom": "2019-02-01",
              "yrke": "Butikkmedarbeider",
              "stillingsprosent": 100
            },
            {
              "tom": "2019-01-31",
              "fom": "2019-01-01",
              "yrke": "Butikkmedarbeider",
              "stillingsprosent": 50
            }
          ],
          "yrke": "Butikkmedarbeider",
          "type": "Arbeidstaker"
        }
      ],
      "inntekt": {
        "beløp": 2500,
        "virksomhet": {
          "identifikator": "889640782",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2019-01"
      }
    },
    {
      "muligeArbeidsforhold": [
        {
          "permisjon": [
            {
              "arsak": "velferdspermisjon",
              "tom": "2019-02-28",
              "fom": "2019-02-01",
              "permisjonsprosent": 100
            }
          ],
          "arbeidsgiver": {
            "identifikator": "889640782",
            "type": "Organisasjon"
          },
          "startdato": "2019-01-01",
          "arbeidsavtaler": [
            {
              "fom": "2019-02-01",
              "yrke": "Butikkmedarbeider",
              "stillingsprosent": 100
            },
            {
              "tom": "2019-01-31",
              "fom": "2019-01-01",
              "yrke": "Butikkmedarbeider",
              "stillingsprosent": 50
            }
          ],
          "yrke": "Butikkmedarbeider",
          "type": "Arbeidstaker"
        }
      ],
      "inntekt": {
        "beløp": 3500,
        "virksomhet": {
          "identifikator": "889640782",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2019-02"
      }
    },
    {
      "muligeArbeidsforhold": [
        {
          "permisjon": [
            {
              "arsak": "velferdspermisjon",
              "tom": "2019-02-28",
              "fom": "2019-02-01",
              "permisjonsprosent": 100
            }
          ],
          "arbeidsgiver": {
            "identifikator": "889640782",
            "type": "Organisasjon"
          },
          "startdato": "2019-01-01",
          "arbeidsavtaler": [
            {
              "fom": "2019-02-01",
              "yrke": "Butikkmedarbeider",
              "stillingsprosent": 100
            },
            {
              "tom": "2019-01-31",
              "fom": "2019-01-01",
              "yrke": "Butikkmedarbeider",
              "stillingsprosent": 50
            }
          ],
          "yrke": "Butikkmedarbeider",
          "type": "Arbeidstaker"
        }
      ],
      "inntekt": {
        "beløp": 2500,
        "virksomhet": {
          "identifikator": "889640782",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2019-02"
      }
    },
    {
      "muligeArbeidsforhold": [
        {
          "permisjon": [
            {
              "arsak": "velferdspermisjon",
              "tom": "2019-02-28",
              "fom": "2019-02-01",
              "permisjonsprosent": 100
            }
          ],
          "arbeidsgiver": {
            "identifikator": "889640782",
            "type": "Organisasjon"
          },
          "startdato": "2019-01-01",
          "arbeidsavtaler": [
            {
              "fom": "2019-02-01",
              "yrke": "Butikkmedarbeider",
              "stillingsprosent": 100
            },
            {
              "tom": "2019-01-31",
              "fom": "2019-01-01",
              "yrke": "Butikkmedarbeider",
              "stillingsprosent": 50
            }
          ],
          "yrke": "Butikkmedarbeider",
          "type": "Arbeidstaker"
        }
      ],
      "inntekt": {
        "beløp": 2500,
        "virksomhet": {
          "identifikator": "889640782",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2019-03"
      }
    },
    {
      "muligeArbeidsforhold": [
        {
          "permisjon": [
            {
              "arsak": "velferdspermisjon",
              "tom": "2019-02-28",
              "fom": "2019-02-01",
              "permisjonsprosent": 100
            }
          ],
          "arbeidsgiver": {
            "identifikator": "889640782",
            "type": "Organisasjon"
          },
          "startdato": "2019-01-01",
          "arbeidsavtaler": [
            {
              "fom": "2019-02-01",
              "yrke": "Butikkmedarbeider",
              "stillingsprosent": 100
            },
            {
              "tom": "2019-01-31",
              "fom": "2019-01-01",
              "yrke": "Butikkmedarbeider",
              "stillingsprosent": 50
            }
          ],
          "yrke": "Butikkmedarbeider",
          "type": "Arbeidstaker"
        }
      ],
      "inntekt": {
        "beløp": -500,
        "virksomhet": {
          "identifikator": "889640782",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2019-03"
      }
    }
  ],
  "arbeidsforhold": [
    {
      "permisjon": [
        {
          "arsak": "velferdspermisjon",
          "tom": "2019-02-28",
          "fom": "2019-02-01",
          "permisjonsprosent": 100
        }
      ],
      "arbeidsgiver": {
        "identifikator": "889640782",
        "type": "Organisasjon"
      },
      "startdato": "2019-01-01",
      "arbeidsavtaler": [
        {
          "fom": "2019-02-01",
          "yrke": "Butikkmedarbeider",
          "stillingsprosent": 100
        },
        {
          "tom": "2019-01-31",
          "fom": "2019-01-01",
          "yrke": "Butikkmedarbeider",
          "stillingsprosent": 50
        }
      ],
      "yrke": "Butikkmedarbeider",
      "type": "Arbeidstaker"
    }
  ],
  "ytelser": [],
  "pensjonEllerTrygd": [],
  "næring": []
}
""".trimIndent()
