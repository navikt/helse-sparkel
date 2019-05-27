package no.nav.helse.domene.arbeid

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
import no.nav.helse.domene.organisasjon.OrganisasjonService
import no.nav.helse.mockedSparkel
import no.nav.helse.oppslag.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.oppslag.inntekt.InntektClient
import no.nav.helse.oppslag.organisasjon.OrganisasjonClient
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.*
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkResponse
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonResponse
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

class ArbeidsforholdComponentTest {

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
            handleRequest(HttpMethod.Get, "/api/arbeidsforhold/${aktørId.aktor}") {
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
            handleRequest(HttpMethod.Get, "/api/arbeidsforhold/${aktørId.aktor}?fom=2019-01-01") {
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
            handleRequest(HttpMethod.Get, "/api/arbeidsforhold/${aktørId.aktor}?fom=foo&tom=2019-01-01") {
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
            handleRequest(HttpMethod.Get, "/api/arbeidsforhold/${aktørId.aktor}?fom=2019-01-01&tom=foo") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `en liste over arbeidsforhold skal returneres`() {
        val arbeidsforholdV3 = mockk<ArbeidsforholdV3>()

        val aktørId = AktørId("1831212532188")

        every {
            arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(match {
                it.ident.ident == aktørId.aktor
            })
        } returns FinnArbeidsforholdPrArbeidstakerResponse().apply {
            with (arbeidsforhold) {
                add(Arbeidsforhold().apply {
                    arbeidsgiver = Organisasjon().apply {
                        orgnummer = "889640782"
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
                })
                add(Arbeidsforhold().apply {
                    arbeidsgiver = Organisasjon().apply {
                        orgnummer = "995298775"
                        navn = "S. VINDEL & SØNN"
                    }
                    arbeidsforholdIDnav = 5678L
                    ansettelsesPeriode = AnsettelsesPeriode().apply {
                        periode = Gyldighetsperiode().apply {
                            this.fom = LocalDate.parse("2015-01-01").toXmlGregorianCalendar()
                            this.tom = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
                        }
                    }
                    with (arbeidsavtale) {
                        add(Arbeidsavtale().apply {
                            fomGyldighetsperiode = LocalDate.parse("2015-01-01").toXmlGregorianCalendar()
                            yrke = Yrker().apply {
                                value = "Butikkmedarbeider"
                            }
                            stillingsprosent = BigDecimal.valueOf(100)
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
                        fomGyldighetsperiode = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
                        yrke = Yrker().apply {
                            value = "Butikkmedarbeider"
                        }
                        stillingsprosent = BigDecimal.valueOf(100)
                    })
                }
            }
        }

        every {
            arbeidsforholdV3.hentArbeidsforholdHistorikk(match { request ->
                request.arbeidsforholdId == 5678L
            })
        } returns HentArbeidsforholdHistorikkResponse().apply {
            arbeidsforhold = Arbeidsforhold().apply {
                with (arbeidsavtale) {
                    add(Arbeidsavtale().apply {
                        fomGyldighetsperiode = LocalDate.parse("2017-01-01").toXmlGregorianCalendar()
                        yrke = Yrker().apply {
                            value = "Butikkmedarbeider"
                        }
                        stillingsprosent = BigDecimal.valueOf(100)
                    })
                    add(Arbeidsavtale().apply {
                        fomGyldighetsperiode = LocalDate.parse("2016-01-01").toXmlGregorianCalendar()
                        tomGyldighetsperiode = LocalDate.parse("2016-12-31").toXmlGregorianCalendar()
                        yrke = Yrker().apply {
                            value = "Butikkmedarbeider"
                        }
                        stillingsprosent = BigDecimal.valueOf(80)
                    })
                    add(Arbeidsavtale().apply {
                        fomGyldighetsperiode = LocalDate.parse("2015-01-01").toXmlGregorianCalendar()
                        tomGyldighetsperiode = LocalDate.parse("2015-12-31").toXmlGregorianCalendar()
                        yrke = Yrker().apply {
                            value = "Butikkmedarbeider"
                        }
                        stillingsprosent = BigDecimal.valueOf(60)
                    })
                }
            }
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                arbeidsforholdService = ArbeidsforholdService(
                        arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3),
                        inntektClient = InntektClient(mockk()),
                        datakvalitetProbe = mockk(relaxed = true)
                ))}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsforhold/${aktørId.aktor}?fom=2017-01-01&tom=2019-01-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertJsonEquals(JSONObject(expectedJson_arbeidsforhold), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `en liste over arbeidsgivere skal returneres`() {
        val arbeidsforholdV3 = mockk<ArbeidsforholdV3>()
        val organisasjonV5 = mockk<OrganisasjonV5>()

        val aktørId = AktørId("1831212532188")

        every {
            arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(match {
                it.ident.ident == aktørId.aktor
            })
        } returns FinnArbeidsforholdPrArbeidstakerResponse().apply {
            with (arbeidsforhold) {
                add(Arbeidsforhold().apply {
                    arbeidsgiver = Organisasjon().apply {
                        orgnummer = "913548221"
                    }
                })
                add(Arbeidsforhold().apply {
                    arbeidsgiver = Organisasjon().apply {
                        orgnummer = "984054564"
                    }
                })
            }
        }

        every {
            organisasjonV5.hentOrganisasjon(match {
                it.orgnummer == "913548221"
            })
        } returns HentOrganisasjonResponse().apply {
            organisasjon = no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Virksomhet().apply {
                orgnummer = "913548221"
                navn = UstrukturertNavn().apply {
                    with(navnelinje) {
                        add("EQUINOR AS")
                        add("AVD STATOIL SOKKELVIRKSOMHET")
                    }
                }
            }
        }

        every {
            organisasjonV5.hentOrganisasjon(match {
                it.orgnummer == "984054564"
            })
        } returns HentOrganisasjonResponse().apply {
            organisasjon = no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Virksomhet().apply {
                orgnummer = "984054564"
                navn = UstrukturertNavn().apply {
                    with(navnelinje) {
                        add("NAV")
                        add("AVD WALDEMAR THRANES GATE")
                    }
                }
            }
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                arbeidsgiverService = ArbeidsgiverService(
                        arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3),
                        organisasjonService = OrganisasjonService(OrganisasjonClient(organisasjonV5))
                ))}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsgivere/${aktørId.aktor}?fom=2017-01-01&tom=2019-01-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertJsonEquals(JSONObject(expectedJson_arbeidsgivere), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `en liste over arbeidsgivere skal returneres selv om organisasjonsoppslag gir feil`() {
        val arbeidsforholdV3 = mockk<ArbeidsforholdV3>()
        val organisasjonV5 = mockk<OrganisasjonV5>()

        val aktørId = AktørId("1831212532188")

        every {
            arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(match {
                it.ident.ident == aktørId.aktor
            })
        } returns FinnArbeidsforholdPrArbeidstakerResponse().apply {
            with (arbeidsforhold) {
                add(Arbeidsforhold().apply {
                    arbeidsgiver = Organisasjon().apply {
                        orgnummer = "913548221"
                    }
                })
                add(Arbeidsforhold().apply {
                    arbeidsgiver = Organisasjon().apply {
                        orgnummer = "984054564"
                    }
                })
            }
        }

        every {
            organisasjonV5.hentOrganisasjon(match {
                it.orgnummer == "913548221"
            })
        } throws(Exception("SOAP fault"))

        every {
            organisasjonV5.hentOrganisasjon(match {
                it.orgnummer == "984054564"
            })
        } throws(Exception("SOAP fault"))

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                arbeidsgiverService = ArbeidsgiverService(
                        arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3),
                        organisasjonService = OrganisasjonService(OrganisasjonClient(organisasjonV5))
                ))}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsgivere/${aktørId.aktor}?fom=2017-01-01&tom=2019-01-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertJsonEquals(JSONObject(expectedJson_arbeidsgivere_uten_navn), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `feil returneres når fom ikke er satt for arbeidsgivere`() {
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
            handleRequest(HttpMethod.Get, "/api/arbeidsgivere/${aktørId.aktor}") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `feil returneres når tom ikke er satt for arbeidsgivere`() {
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
            handleRequest(HttpMethod.Get, "/api/arbeidsgivere/${aktørId.aktor}?fom=2019-01-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `feil returneres når fom er ugyldig for arbeidsgivere`() {
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
            handleRequest(HttpMethod.Get, "/api/arbeidsgivere/${aktørId.aktor}?fom=foo&tom=2019-01-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `feil returneres når tom er ugyldig for arbeidsgivere`() {
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
            handleRequest(HttpMethod.Get, "/api/arbeidsgivere/${aktørId.aktor}?fom=2019-01-01&tom=foo") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expected), JSONObject(response.content))
            }
        }
    }
}

private val expectedJson_arbeidsgivere = """
{
    "arbeidsgivere": [{
        "orgnummer": "913548221",
        "navn": "EQUINOR AS, AVD STATOIL SOKKELVIRKSOMHET",
        "type": "Virksomhet"
    },{
        "orgnummer": "984054564",
        "navn": "NAV, AVD WALDEMAR THRANES GATE",
        "type": "Virksomhet"
    }]
}
""".trimIndent()

private val expectedJson_arbeidsgivere_uten_navn = """
{
    "arbeidsgivere": [{
        "orgnummer": "913548221",
        "type": "Virksomhet"
    },{
        "orgnummer": "984054564",
        "type": "Virksomhet"
    }]
}
""".trimIndent()

private val expectedJson_arbeidsforhold = """
{
    "arbeidsforhold": [{
        "arbeidsgiver": {
            "identifikator": "889640782",
            "type": "Organisasjon"
        },
        "type": "Arbeidstaker",
        "startdato": "2019-01-01",
        "yrke": "Butikkmedarbeider",
        "arbeidsavtaler": [
            {
                "fom":"2019-01-01",
                "yrke":"Butikkmedarbeider",
                "stillingsprosent":100
            }
        ],
        "permisjon": []
    },{
        "arbeidsgiver": {
            "identifikator": "995298775",
            "type": "Organisasjon"
        },
        "type": "Arbeidstaker",
        "startdato": "2015-01-01",
        "sluttdato": "2019-01-01",
        "yrke": "Butikkmedarbeider",
        "arbeidsavtaler": [
            {
                "fom":"2017-01-01",
                "yrke":"Butikkmedarbeider",
                "stillingsprosent":100
            },
            {
                "fom":"2016-01-01",
                "tom":"2016-12-31",
                "yrke":"Butikkmedarbeider",
                "stillingsprosent":80
            },
            {
                "fom":"2015-01-01",
                "tom":"2015-12-31",
                "yrke":"Butikkmedarbeider",
                "stillingsprosent":60
            }
        ],
        "permisjon": []
    }]
}
""".trimIndent()
