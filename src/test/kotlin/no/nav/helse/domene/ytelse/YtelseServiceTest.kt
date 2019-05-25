package no.nav.helse.domene.ytelse

import arrow.core.Either
import arrow.core.right
import arrow.core.success
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.ytelse.domain.*
import no.nav.helse.oppslag.arena.MeldekortUtbetalingsgrunnlagClient
import no.nav.helse.oppslag.infotrygd.InfotrygdSakClient
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagClient
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.*
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Behandlingstema
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeResponse
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.InfotrygdVedtak
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.meldinger.FinnSakListeResponse
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Sak
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Tema
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.meldinger.FinnMeldekortUtbetalingsgrunnlagListeResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class YtelseServiceTest {

    @Test
    fun `skal sammenstille ytelser fra arena og infotrygd`() {
        val aktørregisterService = mockk<AktørregisterService>()
        val infotrygdBeregningsgrunnlagListeClient = mockk<InfotrygdBeregningsgrunnlagClient>()
        val infotrygdSakClient = mockk<InfotrygdSakClient>()
        val meldekortUtbetalingsgrunnlagClient = mockk<MeldekortUtbetalingsgrunnlagClient>()

        val ytelseService = YtelseService(
                aktørregisterService = aktørregisterService,
                infotrygdBeregningsgrunnlagClient = infotrygdBeregningsgrunnlagListeClient,
                infotrygdSakClient = infotrygdSakClient,
                meldekortUtbetalingsgrunnlagClient = meldekortUtbetalingsgrunnlagClient,
                probe = mockk(relaxed = true))

        val aktørId = AktørId("123456789")
        val fødselsnummer = Fødselsnummer("11111111111")
        val identdatoSykepenger = LocalDate.now().minusMonths(1)
        val identdatoForeldrepenger = LocalDate.now().minusDays(28)
        val identdatoEngangstønad = LocalDate.now().minusDays(14)
        val identdatoPleiepenger = LocalDate.now().minusDays(7)
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now()

        every {
            aktørregisterService.fødselsnummerForAktør(aktørId)
        } returns fødselsnummer.value.right()

        every {
            infotrygdSakClient.finnSakListe(fødselsnummer.value, fom, tom)
        } returns sakerFraInfotrygd(identdatoSykepenger, identdatoForeldrepenger, identdatoEngangstønad, identdatoPleiepenger)

        every {
            infotrygdBeregningsgrunnlagListeClient.finnGrunnlagListe(fødselsnummer, fom, tom)
        } returns ytelserFraInfotrygd(identdatoSykepenger, identdatoForeldrepenger, identdatoEngangstønad, identdatoPleiepenger, fom, tom)

        every {
            meldekortUtbetalingsgrunnlagClient.finnMeldekortUtbetalingsgrunnlag(aktørId, fom, tom)
        } returns ytelserFraArena(fom, tom)

        val expectedArena = listOf(
                Ytelse(
                        kilde = Kilde.Arena,
                        tema = "AAP",
                        fom = fom,
                        tom = tom
                ),
                Ytelse(
                        kilde = Kilde.Arena,
                        tema = "DAG",
                        fom = fom,
                        tom = tom
                )
        )
        val expectedInfotrygd = listOf(
                InfotrygdSakOgGrunnlag(
                        sak = InfotrygdSak(
                                sakId = "1",
                                iverksatt = identdatoSykepenger,
                                tema = no.nav.helse.domene.ytelse.domain.Tema.Sykepenger,
                                behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.Sykepenger,
                                opphørerFom = null
                        ),
                        grunnlag = listOf(
                                Beregningsgrunnlag.Sykepenger(
                                        identdato = identdatoSykepenger,
                                        periodeFom = fom,
                                        periodeTom = tom,
                                        behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.Sykepenger,
                                        vedtak = emptyList()
                                )
                        )
                ),
                InfotrygdSakOgGrunnlag(
                        sak = InfotrygdSak(
                                sakId = "2",
                                iverksatt = identdatoForeldrepenger,
                                tema = no.nav.helse.domene.ytelse.domain.Tema.Foreldrepenger,
                                behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.ForeldrepengerMedFødsel,
                                opphørerFom = null
                        ),
                        grunnlag = listOf(
                                Beregningsgrunnlag.Foreldrepenger(
                                        identdato = identdatoForeldrepenger,
                                        periodeFom = fom,
                                        periodeTom = tom,
                                        behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.ForeldrepengerMedFødsel,
                                        vedtak = emptyList()
                                )
                        )
                ),
                InfotrygdSakOgGrunnlag(
                        sak = InfotrygdSak(
                                sakId = "3",
                                iverksatt = identdatoEngangstønad,
                                tema = no.nav.helse.domene.ytelse.domain.Tema.Foreldrepenger,
                                behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.EngangstønadMedFødsel,
                                opphørerFom = null
                        ),
                        grunnlag = listOf(
                                Beregningsgrunnlag.Engangstønad(
                                        identdato = identdatoEngangstønad,
                                        periodeFom = fom,
                                        periodeTom = tom,
                                        behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.EngangstønadMedFødsel,
                                        vedtak = emptyList()
                                )
                        )
                ),
                InfotrygdSakOgGrunnlag(
                        sak = InfotrygdSak(
                                sakId = "4",
                                iverksatt = identdatoPleiepenger,
                                tema = no.nav.helse.domene.ytelse.domain.Tema.PårørendeSykdom,
                                behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.Pleiepenger,
                                opphørerFom = null
                        ),
                        grunnlag = listOf(
                                Beregningsgrunnlag.PårørendeSykdom(
                                        identdato = identdatoPleiepenger,
                                        periodeFom = fom,
                                        periodeTom = tom,
                                        behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.Pleiepenger,
                                        vedtak = emptyList()
                                )
                        )
                )
        )
        val actual = ytelseService.finnYtelser(aktørId, fom, tom)

        assertTrue(actual is Either.Right)
        actual as Either.Right
        assertEquals(expectedArena, actual.b.arena)
        assertEquals(expectedInfotrygd, actual.b.infotrygd)
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
    }.success()

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
                })
            }.success()

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
    }.success()
}
