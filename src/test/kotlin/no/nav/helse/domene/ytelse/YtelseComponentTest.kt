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
import no.nav.helse.domene.ytelse.arena.ArenaService
import no.nav.helse.domene.ytelse.infotrygd.InfotrygdService
import no.nav.helse.mockedSparkel
import no.nav.helse.oppslag.arena.MeldekortUtbetalingsgrunnlagClient
import no.nav.helse.oppslag.infotrygd.InfotrygdSakClient
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagClient
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.*
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeResponse
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.InfotrygdSakV1
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.InfotrygdVedtak
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Status
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.meldinger.FinnSakListeResponse
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.MeldekortUtbetalingsgrunnlagV1
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Sak
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Tema
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.meldinger.FinnMeldekortUtbetalingsgrunnlagListeResponse
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.xml.namespace.QName
import javax.xml.soap.SOAPConstants
import javax.xml.soap.SOAPFactory
import javax.xml.ws.soap.SOAPFaultException

class YtelseComponentTest {

    @Test
    fun `skal gi liste over ytelser`() {
        val infotrygdBeregningsgrunnlagV1 = mockk<InfotrygdBeregningsgrunnlagV1>()
        val infotrygdSakV1 = mockk<InfotrygdSakV1>()
        val meldekortUtbetalingsgrunnlagV1 = mockk<MeldekortUtbetalingsgrunnlagV1>()
        val aktørregisterService = mockk<AktørregisterService>()

        val fnr = Fødselsnummer("11111111111")
        val aktørId = AktørId("123456789")
        val identdatoSykepenger = LocalDate.of(2019, 5, 28)
        val identdatoForeldrepenger = LocalDate.of(2019, 5, 14)
        val identdatoEngangstønad = LocalDate.of(2019, 5, 7)
        val identdatoPleiepenger = LocalDate.of(2019, 5, 1)
        val fom = LocalDate.of(2019, 5, 1)
        val tom = LocalDate.of(2019, 5, 31)

        every {
            aktørregisterService.fødselsnummerForAktør(aktørId)
        } returns fnr.value.right()

        every {
            infotrygdSakV1.finnSakListe(match { request ->
                request.personident == fnr.value
                        && request.periode.fom.toLocalDate() == fom
                        && request.periode.tom.toLocalDate() == tom
            })
        } returns sakerFraInfotrygd(identdatoSykepenger, identdatoForeldrepenger, identdatoEngangstønad, identdatoPleiepenger)

        every {
            infotrygdBeregningsgrunnlagV1.finnGrunnlagListe(match { request ->
                request.personident == fnr.value
                        && request.fom.toLocalDate() == fom
                        && request.tom.toLocalDate() == tom
            })
        } returns ytelserFraInfotrygd(identdatoSykepenger, identdatoForeldrepenger, identdatoEngangstønad, identdatoPleiepenger, fom, tom)

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

        withTestApplication({
            mockedSparkel(
                    jwtIssuer = "test issuer",
                    jwkProvider = jwkStub.stubbedJwkProvider(),
                    ytelseService = YtelseService(
                            aktørregisterService = aktørregisterService,
                            infotrygdService = InfotrygdService(
                                    infotrygdBeregningsgrunnlagClient = InfotrygdBeregningsgrunnlagClient(infotrygdBeregningsgrunnlagV1),
                                    infotrygdSakClient = InfotrygdSakClient(infotrygdSakV1),
                                    probe = mockk(relaxed = true)
                            ),
                            arenaService = ArenaService(
                                    meldekortUtbetalingsgrunnlagClient = MeldekortUtbetalingsgrunnlagClient(meldekortUtbetalingsgrunnlagV1)
                            )
                    )
            )
        }) {
            handleRequest(HttpMethod.Get, "/api/ytelser/${aktørId.aktor}?fom=$fom&tom=$tom") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertJsonEquals(JSONObject(expectedJson), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `skal returnere 503 når infotrygd er utilgjengelig`() {
        val infotrygdBeregningsgrunnlagV1 = mockk<InfotrygdBeregningsgrunnlagV1>()
        val infotrygdSakV1 = mockk<InfotrygdSakV1>()
        val meldekortUtbetalingsgrunnlagV1 = mockk<MeldekortUtbetalingsgrunnlagV1>()
        val aktørregisterService = mockk<AktørregisterService>()

        val fnr = Fødselsnummer("11111111111")
        val aktørId = AktørId("123456789")
        val fom = LocalDate.of(2019, 5, 1)
        val tom = LocalDate.of(2019, 5, 31)

        every {
            aktørregisterService.fødselsnummerForAktør(aktørId)
        } returns fnr.value.right()

        every {
            infotrygdSakV1.finnSakListe(match { request ->
                request.personident == fnr.value
                        && request.periode.fom.toLocalDate() == fom
                        && request.periode.tom.toLocalDate() == tom
            })
        } throws SOAPFaultException(SOAPFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createFault("Basene i Infotrygd er ikke tilgjengelige", QName("nameSpaceURI", "ERROR")))

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
                        aktørregisterService = aktørregisterService,
                        infotrygdService = InfotrygdService(
                                infotrygdBeregningsgrunnlagClient = InfotrygdBeregningsgrunnlagClient(infotrygdBeregningsgrunnlagV1),
                                infotrygdSakClient = InfotrygdSakClient(infotrygdSakV1),
                                probe = mockk(relaxed = true)
                        ),
                        arenaService = ArenaService(
                                meldekortUtbetalingsgrunnlagClient = MeldekortUtbetalingsgrunnlagClient(meldekortUtbetalingsgrunnlagV1)
                        )
                )
        )}) {
            handleRequest(HttpMethod.Get, "/api/ytelser/${aktørId.aktor}?fom=$fom&tom=$tom") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.ServiceUnavailable, response.status())
                assertJsonEquals(JSONObject(expectedJson_service_unavailable), JSONObject(response.content))
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

    private fun sakerFraInfotrygd(identdatoSykepenger: LocalDate, identdatoForeldrepenger: LocalDate, identdatoEngangstønad: LocalDate, identdatoPleiepenger: LocalDate) =
            FinnSakListeResponse().apply {
                this.vedtakListe.add(InfotrygdVedtak().apply {
                    this.sakId = "1"
                    this.iverksatt = identdatoSykepenger.toXmlGregorianCalendar()
                    this.tema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Tema().apply {
                        value = "SP"
                    }
                    this.behandlingstema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Behandlingstema().apply {
                        value = "SP"
                    }
                    this.status = Status().apply {
                        value = "I"
                    }
                })
                this.vedtakListe.add(InfotrygdVedtak().apply {
                    this.sakId = "2"
                    this.iverksatt = identdatoForeldrepenger.toXmlGregorianCalendar()
                    this.tema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Tema().apply {
                        value = "FA"
                    }
                    this.behandlingstema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Behandlingstema().apply {
                        value = "FØ"
                    }
                    this.status = Status().apply {
                        value = "A"
                    }
                })
                this.vedtakListe.add(InfotrygdVedtak().apply {
                    this.sakId = "3"
                    this.iverksatt = identdatoEngangstønad.toXmlGregorianCalendar()
                    this.tema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Tema().apply {
                        value = "FA"
                    }
                    this.behandlingstema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Behandlingstema().apply {
                        value = "FE"
                    }
                    this.status = Status().apply {
                        value = "A"
                    }
                })
                this.vedtakListe.add(InfotrygdVedtak().apply {
                    this.sakId = "4"
                    this.iverksatt = identdatoPleiepenger.toXmlGregorianCalendar()
                    this.tema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Tema().apply {
                        value = "BS"
                    }
                    this.behandlingstema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Behandlingstema().apply {
                        value = "PN"
                    }
                    this.status = Status().apply {
                        value = "A"
                    }
                })
            }

    private fun ytelserFraInfotrygd(identdatoSykepenger: LocalDate, identdatoForeldrepenger: LocalDate, identdatoEngangstønad: LocalDate, identdatoPleiepenger: LocalDate, fom: LocalDate, tom: LocalDate) = FinnGrunnlagListeResponse().apply {
        with (foreldrepengerListe) {
            add(Foreldrepenger().apply {
                periode = Periode().apply {
                    this.fom = fom.toXmlGregorianCalendar()
                    this.tom = tom.toXmlGregorianCalendar()
                }
                behandlingstema = Behandlingstema().apply {
                    value = "FØ"
                }
                this.identdato = identdatoForeldrepenger.toXmlGregorianCalendar()
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
                this.identdato = identdatoSykepenger.toXmlGregorianCalendar()
            })
        }
        with (engangstoenadListe) {
            add(Engangsstoenad().apply {
                periode = Periode().apply {
                    this.fom = fom.toXmlGregorianCalendar()
                    this.tom = tom.toXmlGregorianCalendar()
                }
                behandlingstema = Behandlingstema().apply {
                    value = "FE"
                }
                this.identdato = identdatoEngangstønad.toXmlGregorianCalendar()
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
                this.identdato = identdatoPleiepenger.toXmlGregorianCalendar()
            })
        }
    }
}

private val expectedJson_service_unavailable = """
{
  "feilmelding": "Service is unavailable"
}
""".trimIndent()

private val expectedJson = """
{
  "arena": [
    {
      "kilde": "Arena",
      "tom": "2019-05-31",
      "tema": "AAP",
      "fom": "2019-05-01"
    },
    {
      "kilde": "Arena",
      "tom": "2019-05-31",
      "tema": "DAG",
      "fom": "2019-05-01"
    }
  ],
  "infotrygd": [
      {
        "sak": {
          "type": "Vedtak",
          "tema": "Sykepenger",
          "behandlingstema": "Sykepenger",
          "iverksatt": "2019-05-28",
          "ikkeStartet": true
        },
        "grunnlag": {
          "type": "Sykepenger",
          "periodeTom": "2019-05-31",
          "vedtak": [],
          "behandlingstema": "Sykepenger",
          "identdato": "2019-05-28",
          "periodeFom": "2019-05-01"
        }
      },
      {
        "sak": {
          "type": "Vedtak",
          "tema": "Foreldrepenger",
          "behandlingstema": "ForeldrepengerMedFødsel",
          "iverksatt": "2019-05-14",
          "ikkeStartet": false
        },
        "grunnlag": {
          "type": "Foreldrepenger",
          "periodeTom": "2019-05-31",
          "vedtak": [],
          "behandlingstema": "ForeldrepengerMedFødsel",
          "identdato": "2019-05-14",
          "periodeFom": "2019-05-01"
        }
      },
      {
        "sak": {
          "type": "Vedtak",
          "tema": "Foreldrepenger",
          "behandlingstema": "EngangstønadMedFødsel",
          "iverksatt": "2019-05-07",
          "ikkeStartet": false
        },
        "grunnlag": {
          "type": "Engangstønad",
          "periodeTom": "2019-05-31",
          "vedtak": [],
          "behandlingstema": "EngangstønadMedFødsel",
          "identdato": "2019-05-07",
          "periodeFom": "2019-05-01"
        }
      },
      {
        "sak": {
          "type": "Vedtak",
          "tema": "PårørendeSykdom",
          "behandlingstema": "Pleiepenger",
          "iverksatt": "2019-05-01",
          "ikkeStartet": false
        },
        "grunnlag": {
          "type": "PårørendeSykdom",
          "periodeTom": "2019-05-31",
          "vedtak": [],
          "behandlingstema": "Pleiepenger",
          "identdato": "2019-05-01",
          "periodeFom": "2019-05-01"
        }
      }
    ]
}
""".trimIndent()
