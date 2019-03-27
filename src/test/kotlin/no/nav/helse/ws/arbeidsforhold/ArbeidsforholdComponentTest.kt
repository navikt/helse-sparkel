package no.nav.helse.ws.arbeidsforhold

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
import no.nav.helse.ws.organisasjon.OrganisasjonClient
import no.nav.helse.ws.organisasjon.OrganisasjonService
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.AnsettelsesPeriode
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Gyldighetsperiode
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonResponse
import org.json.JSONObject
import org.junit.jupiter.api.Test
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
                        orgnummer = "889640782"
                    }
                    ansettelsesPeriode = AnsettelsesPeriode().apply {
                        periode = Gyldighetsperiode().apply {
                            this.fom = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
                        }
                    }
                })
                add(Arbeidsforhold().apply {
                    arbeidsgiver = Organisasjon().apply {
                        orgnummer = "995298775"
                        navn = "S. VINDEL & SØNN"
                    }
                    ansettelsesPeriode = AnsettelsesPeriode().apply {
                        periode = Gyldighetsperiode().apply {
                            this.fom = LocalDate.parse("2015-01-01").toXmlGregorianCalendar()
                            this.tom = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
                        }
                    }
                })
            }
        }

        every {
            organisasjonV5.hentOrganisasjon(match {
                it.orgnummer == "889640782"
            })
        } returns HentOrganisasjonResponse().apply {
            organisasjon = no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon().apply {
                orgnummer = "889640782"
                navn = UstrukturertNavn().apply {
                    with (navnelinje) {
                        add("MATBUTIKKEN AS")
                    }
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
                        organisasjonService = OrganisasjonService(OrganisasjonClient(organisasjonV5))
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
            organisasjon = no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon().apply {
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
            organisasjon = no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon().apply {
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
                arbeidsforholdService = ArbeidsforholdService(
                        arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3),
                        organisasjonService = OrganisasjonService(OrganisasjonClient(organisasjonV5))
                ))}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsgivere/${aktørId.aktor}?fom=2017-01-01&tom=2019-01-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(200, response.status()?.value)
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
            organisasjonV5.hentNoekkelinfoOrganisasjon(match {
                it.orgnummer == "913548221"
            })
        } throws(Exception("SOAP fault"))

        every {
            organisasjonV5.hentNoekkelinfoOrganisasjon(match {
                it.orgnummer == "984054564"
            })
        } throws(Exception("SOAP fault"))

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                arbeidsforholdService = ArbeidsforholdService(
                        arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3),
                        organisasjonService = OrganisasjonService(OrganisasjonClient(organisasjonV5))
                ))}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsgivere/${aktørId.aktor}?fom=2017-01-01&tom=2019-01-01") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(200, response.status()?.value)
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
        "navn": "EQUINOR AS, AVD STATOIL SOKKELVIRKSOMHET"
    },{
        "orgnummer": "984054564",
        "navn": "NAV, AVD WALDEMAR THRANES GATE"
    }]
}
""".trimIndent()

private val expectedJson_arbeidsgivere_uten_navn = """
{
    "arbeidsgivere": [{
        "orgnummer": "913548221"
    },{
        "orgnummer": "984054564"
    }]
}
""".trimIndent()

private val expectedJson_arbeidsforhold = """
{
    "arbeidsforhold": [{
        "arbeidsgiver": {
            "orgnummer": "889640782",
            "navn": "MATBUTIKKEN AS"
        },
        "startdato": "2019-01-01"
    },{
        "arbeidsgiver": {
            "orgnummer": "995298775",
            "navn": "S. VINDEL & SØNN"
        },
        "startdato": "2015-01-01",
        "sluttdato": "2019-01-01"
    }]
}
""".trimIndent()
