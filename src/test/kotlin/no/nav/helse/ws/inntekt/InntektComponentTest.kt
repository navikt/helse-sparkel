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
import no.nav.helse.mockedSparkel
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.inntekt.client.InntektClient
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class InntektComponentTest {

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
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}/beregningsgrunnlag") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.BadRequest, response.status())
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
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}/beregningsgrunnlag?fom=2019-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `feil returneres når fom er ugyldig`() {
        val aktørId = AktørId("1831212532188")

        val expected = """
            {
                "feilmelding": "fom must be specified as yyyy-mm"
            }
        """.trimIndent()

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider()
        )}) {
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}/beregningsgrunnlag?fom=foo&tom=2019-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `feil returneres når tom er ugyldig`() {
        val aktørId = AktørId("1831212532188")

        val expected = """
            {
                "feilmelding": "tom must be specified as yyyy-mm"
            }
        """.trimIndent()

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider()
        )}) {
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}/beregningsgrunnlag?fom=2019-01&tom=foo") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `skal svare med liste av inntekter`() {
        val inntektV3 = mockk<InntektV3>()

        val aktørId = AktørId("11987654321")
        val fom = YearMonth.parse("2019-01")

        val virksomhet1 = "889640782"
        val virksomhet2 = "912998827"
        val virksomhet3 = "995298775"
        val expected = listeMedTreInntekter(aktørId, fom, virksomhet1, virksomhet2, virksomhet3)

        every {
            inntektV3.hentInntektListeBolk(match {
                it.identListe.size == 1 && (it.identListe[0] as AktoerId).aktoerId == aktørId.aktor
            })
        } returns expected

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                inntektService = InntektService(InntektClient(inntektV3)))}) {
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}/beregningsgrunnlag?fom=2019-01&tom=2019-03") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertJsonEquals(JSONObject(expectedJson), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `skal svare med feil ved feil`() {
        val inntektV3 = mockk<InntektV3>()

        val aktørId = AktørId("11987654321")

        every {
            inntektV3.hentInntektListeBolk(match {
                it.identListe.size == 1 && (it.identListe[0] as AktoerId).aktoerId == aktørId.aktor
            })
        } throws (Exception("SOAP fault"))

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                inntektService = InntektService(InntektClient(inntektV3)))}) {
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}/beregningsgrunnlag?fom=2019-01&tom=2019-02") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError.value, response.status()?.value)
                assertJsonEquals(JSONObject(expectedJson_fault), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `feil returneres når fom ikke er satt for sammenligningsgrunnlag`() {
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
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}/sammenligningsgrunnlag") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `feil returneres når tom ikke er satt for sammenligningsgrunnlag`() {
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
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}/sammenligningsgrunnlag?fom=2019-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `feil returneres når fom er ugyldig for sammenligningsgrunnlag`() {
        val aktørId = AktørId("1831212532188")

        val expected = """
            {
                "feilmelding": "fom must be specified as yyyy-mm"
            }
        """.trimIndent()

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider()
        )}) {
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}/sammenligningsgrunnlag?fom=foo&tom=2019-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `feil returneres når tom er ugyldig for sammenligningsgrunnlag`() {
        val aktørId = AktørId("1831212532188")

        val expected = """
            {
                "feilmelding": "tom must be specified as yyyy-mm"
            }
        """.trimIndent()

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider()
        )}) {
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}/sammenligningsgrunnlag?fom=2019-01&tom=foo") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    private fun inntektUtenOpptjeningsperiode(aktørId: AktørId,
                                              virksomhet: Organisasjon,
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
                                             virksomhet: Organisasjon,
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

    private fun listeMedTreInntekter(aktørId: AktørId, fom: YearMonth, virksomhet1: String, virksomhet2: String, virksomhet3: String) = HentInntektListeBolkResponse().apply {
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
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = virksomhet1
                                        },
                                        periode = YearMonth.of(2019, 1),
                                        opptjeningsperiodeFom = LocalDate.parse("2019-01-01"),
                                        opptjeningsperiodeTom = LocalDate.parse("2019-01-31"),
                                        beløp = 2500
                                ))
                                add(inntektUtenOpptjeningsperiode(
                                        aktørId = aktørId,
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = virksomhet2
                                        },
                                        periode = YearMonth.of(2019, 2),
                                        beløp = 3500
                                ))
                                add(inntektMedOpptjeningsperiode(
                                        aktørId = aktørId,
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = virksomhet3
                                        },
                                        periode = YearMonth.of(2019, 3),
                                        opptjeningsperiodeFom = LocalDate.parse("2019-03-01"),
                                        opptjeningsperiodeTom = LocalDate.parse("2019-03-31"),
                                        beløp = 2500
                                ))
                            }
                        }
                    })
                }
            })
        }
    }

    @Test
    fun `skal svare med liste av inntekter for sammenligningsgrunnlag`() {
        val inntektV3 = mockk<InntektV3>()

        val aktørId = AktørId("11987654321")
        val fom = YearMonth.parse("2019-01")

        val virksomhet1 = "889640782"
        val virksomhet2 = "912998827"
        val virksomhet3 = "995298775"
        val expected = listeMedTreInntekter(aktørId, fom, virksomhet1, virksomhet2, virksomhet3)

        every {
            inntektV3.hentInntektListeBolk(match {
                it.identListe.size == 1 && (it.identListe[0] as AktoerId).aktoerId == aktørId.aktor
            })
        } returns expected

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                inntektService = InntektService(InntektClient(inntektV3)))}) {
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}/sammenligningsgrunnlag?fom=2019-01&tom=2019-03") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertJsonEquals(JSONObject(expectedJson), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `skal svare med feil ved feil for sammenligningsgrunnlag`() {
        val inntektV3 = mockk<InntektV3>()

        val aktørId = AktørId("11987654321")

        every {
            inntektV3.hentInntektListeBolk(match {
                it.identListe.size == 1 && (it.identListe[0] as AktoerId).aktoerId == aktørId.aktor
            })
        } throws (Exception("SOAP fault"))

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                inntektService = InntektService(InntektClient(inntektV3)))}) {
            handleRequest(HttpMethod.Get, "/api/inntekt/${aktørId.aktor}/sammenligningsgrunnlag?fom=2019-01&tom=2019-02") {
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
    "inntekter": [{
        "arbeidsgiver": {
            "identifikator": "889640782",
            "type": "Organisasjon"
        },
        "beløp": 2500,
        "utbetalingsperiode": "2019-01",
        "ytelse": false
    }, {
        "arbeidsgiver": {
            "identifikator": "912998827",
            "type": "Organisasjon"
        },
        "beløp": 3500,
        "utbetalingsperiode": "2019-02",
        "ytelse": false
    }, {
        "arbeidsgiver": {
            "identifikator": "995298775",
            "type": "Organisasjon"
        },
        "beløp": 2500,
        "utbetalingsperiode": "2019-03",
        "ytelse": false
    }]
}
""".trimIndent()

private val expectedJson_fault = """
{
    "feilmelding":"Unknown error"
}
""".trimIndent()
