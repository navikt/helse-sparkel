package no.nav.helse.domene.ytelse

import arrow.core.right
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
import no.nav.helse.common.toLocalDate
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.infotrygd.InfotrygdBeregningsgrunnlagService
import no.nav.helse.mockedSparkel
import no.nav.helse.oppslag.arena.MeldekortUtbetalingsgrunnlagClient
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagListeClient
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.*
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeResponse
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.MeldekortUtbetalingsgrunnlagV1
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Sak
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Tema
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.meldinger.FinnMeldekortUtbetalingsgrunnlagListeResponse
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class YtelseComponentTest {

    @Test
    fun `skal gi liste over ytelser`() {
        val infotrygdBeregningsgrunnlagV1 = mockk<InfotrygdBeregningsgrunnlagV1>()
        val meldekortUtbetalingsgrunnlagV1 = mockk<MeldekortUtbetalingsgrunnlagV1>()
        val aktørregisterService = mockk<AktørregisterService>()

        val fnr = Fødselsnummer("11111111111")
        val aktørId = AktørId("123456789")
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now()

        every {
            aktørregisterService.fødselsnummerForAktør(aktørId)
        } returns fnr.value.right()

        every {
            infotrygdBeregningsgrunnlagV1.finnGrunnlagListe(match { request ->
                request.personident == fnr.value
                        && request.fom.toLocalDate() == fom
                        && request.tom.toLocalDate() == tom
            })
        } returns ytelserFraInfotrygd(fom, tom)

        every {
            meldekortUtbetalingsgrunnlagV1.finnMeldekortUtbetalingsgrunnlagListe(match { request ->
                (request.ident as AktoerId).aktoerId == aktørId.aktor
                        && request.periode.fom.toLocalDate() == fom
                        && request.periode.tom.toLocalDate() == tom
                        && request.temaListe.any { it.value == "DAG" }
                        && request.temaListe.any { it.value == "AAP" }
            })
        } returns ytelserFraArena(fom, tom)

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                ytelseService = YtelseService(
                        infotrygdBeregningsgrunnlagService = InfotrygdBeregningsgrunnlagService(
                                infotrygdClient = InfotrygdBeregningsgrunnlagListeClient(infotrygdBeregningsgrunnlagV1),
                                aktørregisterService = aktørregisterService
                        ),
                        meldekortUtbetalingsgrunnlagClient = MeldekortUtbetalingsgrunnlagClient(meldekortUtbetalingsgrunnlagV1)
                )
                )}) {
            handleRequest(HttpMethod.Get, "/api/ytelser/${aktørId.aktor}?fom=$fom&tom=$tom") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertJsonEquals(JSONObject(expectedJson), JSONObject(response.content))
            }
        }
    }

    private fun ytelserFraArena(fom: LocalDate, tom: LocalDate) = FinnMeldekortUtbetalingsgrunnlagListeResponse().apply {
        with (meldekortUtbetalingsgrunnlagListe) {
            add(Sak().apply {
                tema = Tema().apply {
                    value = "AAP"
                }
                with(vedtakListe) {
                    add(no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Vedtak().apply {
                        vedtaksperiode = no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Periode().apply {
                            this.fom = fom.toXmlGregorianCalendar()
                            this.tom = tom.toXmlGregorianCalendar()
                        }
                    })
                }
            })
            add(Sak().apply {
                tema = Tema().apply {
                    value = "DAG"
                }
                with(vedtakListe) {
                    add(no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Vedtak().apply {
                        vedtaksperiode = no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Periode().apply {
                            this.fom = fom.toXmlGregorianCalendar()
                            this.tom = tom.toXmlGregorianCalendar()
                        }
                    })
                }
            })
        }
    }

    private fun ytelserFraInfotrygd(fom: LocalDate, tom: LocalDate) = FinnGrunnlagListeResponse().apply {
        with (foreldrepengerListe) {
            add(Foreldrepenger().apply {
                periode = Periode().apply {
                    this.fom = fom.toXmlGregorianCalendar()
                    this.tom = tom.toXmlGregorianCalendar()
                }
                behandlingstema = Behandlingstema().apply {
                    value = "FP"
                }
            })
        }
        with (sykepengerListe) {
            add(Sykepenger().apply {
                periode = Periode().apply {
                    this.fom = fom.toXmlGregorianCalendar()
                    this.tom = tom.toXmlGregorianCalendar()
                }
                behandlingstema = Behandlingstema().apply {
                    value = "SP"
                }
            })
        }
        with (engangstoenadListe) {
            add(Engangsstoenad().apply {
                periode = Periode().apply {
                    this.fom = fom.toXmlGregorianCalendar()
                    this.tom = tom.toXmlGregorianCalendar()
                }
                behandlingstema = Behandlingstema().apply {
                    value = "FØ"
                }
            })
        }
        with (paaroerendeSykdomListe) {
            add(PaaroerendeSykdom().apply {
                periode = Periode().apply {
                    this.fom = fom.toXmlGregorianCalendar()
                    this.tom = tom.toXmlGregorianCalendar()
                }
                behandlingstema = Behandlingstema().apply {
                    value = "PN"
                }
            })
        }
    }
}

private val expectedJson = """
{
  "ytelser": [
    {
      "kilde": "Arena",
      "tom": "2019-05-16",
      "tema": "AAP",
      "fom": "2019-04-16"
    },
    {
      "kilde": "Arena",
      "tom": "2019-05-16",
      "tema": "DAG",
      "fom": "2019-04-16"
    },
    {
      "kilde": "Infotrygd",
      "tom": "2019-05-16",
      "tema": "SP",
      "fom": "2019-04-16"
    },
    {
      "kilde": "Infotrygd",
      "tom": "2019-05-16",
      "tema": "FP",
      "fom": "2019-04-16"
    },
    {
      "kilde": "Infotrygd",
      "tom": "2019-05-16",
      "tema": "FØ",
      "fom": "2019-04-16"
    },
    {
      "kilde": "Infotrygd",
      "tom": "2019-05-16",
      "tema": "PN",
      "fom": "2019-04-16"
    }
  ]
}
""".trimIndent()
