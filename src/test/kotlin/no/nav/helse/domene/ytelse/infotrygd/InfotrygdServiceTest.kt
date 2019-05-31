package no.nav.helse.domene.ytelse.infotrygd

import arrow.core.Either
import arrow.core.success
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.ytelse.domain.*
import no.nav.helse.domene.ytelse.domain.Behandlingstema
import no.nav.helse.oppslag.infotrygd.InfotrygdSakClient
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagClient
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.*
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeResponse
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.InfotrygdVedtak
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.meldinger.FinnSakListeResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InfotrygdServiceTest {

    @Test
    fun `skal sammenstille ytelser fra arena og infotrygd`() {
        val infotrygdBeregningsgrunnlagClient = mockk<InfotrygdBeregningsgrunnlagClient>()
        val infotrygdSakClient = mockk<InfotrygdSakClient>()

        val infotrygdService = InfotrygdService(
                infotrygdBeregningsgrunnlagClient = infotrygdBeregningsgrunnlagClient,
                infotrygdSakClient = infotrygdSakClient,
                probe = mockk(relaxed = true)
        )

        val fødselsnummer = Fødselsnummer("11111111111")
        val identdatoSykepenger = LocalDate.now().minusMonths(1)
        val identdatoForeldrepenger = LocalDate.now().minusDays(28)
        val identdatoEngangstønad = LocalDate.now().minusDays(14)
        val identdatoPleiepenger = LocalDate.now().minusDays(7)
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now()

        every {
            infotrygdSakClient.finnSakListe(fødselsnummer.value, fom, tom)
        } returns saker(identdatoSykepenger, identdatoForeldrepenger, identdatoEngangstønad, identdatoPleiepenger)

        every {
            infotrygdBeregningsgrunnlagClient.finnGrunnlagListe(fødselsnummer.value, fom, tom)
        } returns beregningsgrunnlag(identdatoSykepenger, identdatoForeldrepenger, identdatoEngangstønad, identdatoPleiepenger, fom, tom)

        val expected = listOf(
                InfotrygdSakOgGrunnlag(
                        sak = InfotrygdSak.Vedtak(
                                sakId = "1",
                                iverksatt = identdatoSykepenger,
                                tema = Tema.Sykepenger,
                                behandlingstema = Behandlingstema.Sykepenger,
                                opphørerFom = null
                        ),
                        grunnlag = Beregningsgrunnlag.Sykepenger(
                                identdato = identdatoSykepenger,
                                periodeFom = fom,
                                periodeTom = tom,
                                behandlingstema = Behandlingstema.Sykepenger,
                                vedtak = emptyList()
                        )
                ),
                InfotrygdSakOgGrunnlag(
                        sak = InfotrygdSak.Vedtak(
                                sakId = "2",
                                iverksatt = identdatoForeldrepenger,
                                tema = Tema.Foreldrepenger,
                                behandlingstema = Behandlingstema.ForeldrepengerMedFødsel,
                                opphørerFom = null
                        ),
                        grunnlag = Beregningsgrunnlag.Foreldrepenger(
                                identdato = identdatoForeldrepenger,
                                periodeFom = fom,
                                periodeTom = tom,
                                behandlingstema = Behandlingstema.ForeldrepengerMedFødsel,
                                vedtak = emptyList()
                        )
                ),
                InfotrygdSakOgGrunnlag(
                        sak = InfotrygdSak.Vedtak(
                                sakId = "3",
                                iverksatt = identdatoEngangstønad,
                                tema = Tema.Foreldrepenger,
                                behandlingstema = Behandlingstema.EngangstønadMedFødsel,
                                opphørerFom = null
                        ),
                        grunnlag = Beregningsgrunnlag.Engangstønad(
                                identdato = identdatoEngangstønad,
                                periodeFom = fom,
                                periodeTom = tom,
                                behandlingstema = Behandlingstema.EngangstønadMedFødsel,
                                vedtak = emptyList()
                        )
                ),
                InfotrygdSakOgGrunnlag(
                        sak = InfotrygdSak.Vedtak(
                                sakId = "4",
                                iverksatt = identdatoPleiepenger,
                                tema = Tema.PårørendeSykdom,
                                behandlingstema = Behandlingstema.Pleiepenger,
                                opphørerFom = null
                        ),
                        grunnlag = Beregningsgrunnlag.PårørendeSykdom(
                                identdato = identdatoPleiepenger,
                                periodeFom = fom,
                                periodeTom = tom,
                                behandlingstema = Behandlingstema.Pleiepenger,
                                vedtak = emptyList()
                        )
                )
        )
        val actual = infotrygdService.finnSakerOgGrunnlag(fødselsnummer, fom, tom)

        Assertions.assertTrue(actual is Either.Right)
        actual as Either.Right
        Assertions.assertEquals(expected, actual.b)
    }

    private fun saker(identdatoSykepenger: LocalDate, identdatoForeldrepenger: LocalDate, identdatoEngangstønad: LocalDate, identdatoPleiepenger: LocalDate) =
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

    private fun beregningsgrunnlag(identdatoSykepenger: LocalDate, identdatoForeldrepenger: LocalDate, identdatoEngangstønad: LocalDate, identdatoPleiepenger: LocalDate, fom: LocalDate, tom: LocalDate) = FinnGrunnlagListeResponse().apply {
        with (foreldrepengerListe) {
            add(Foreldrepenger().apply {
                periode = Periode().apply {
                    this.fom = fom.toXmlGregorianCalendar()
                    this.tom = tom.toXmlGregorianCalendar()
                }
                behandlingstema = no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Behandlingstema().apply {
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
                behandlingstema = no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Behandlingstema().apply {
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
                behandlingstema = no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Behandlingstema().apply {
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
                behandlingstema = no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Behandlingstema().apply {
                    value = "PN"
                }
                this.identdato = identdatoPleiepenger.toXmlGregorianCalendar()
            })
        }
    }.success()
}
