package no.nav.helse.domene.ytelse

import arrow.core.Either
import arrow.core.right
import arrow.core.success
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.infotrygd.InfotrygdBeregningsgrunnlagService
import no.nav.helse.domene.ytelse.domain.Ytelse
import no.nav.helse.oppslag.arena.MeldekortUtbetalingsgrunnlagClient
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.*
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeResponse
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
        val infotrygdBeregningsgrunnlagService = mockk<InfotrygdBeregningsgrunnlagService>()
        val meldekortUtbetalingsgrunnlagClient = mockk<MeldekortUtbetalingsgrunnlagClient>()

        val ytelseService = YtelseService(infotrygdBeregningsgrunnlagService, meldekortUtbetalingsgrunnlagClient)

        val aktørId = AktørId("123456789")
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now()

        every {
            infotrygdBeregningsgrunnlagService.finnGrunnlagListe(aktørId, fom, tom)
        } returns ytelserFraInfotrygd(fom, tom)

        every {
            meldekortUtbetalingsgrunnlagClient.finnMeldekortUtbetalingsgrunnlag(aktørId, fom, tom)
        } returns ytelserFraArena(fom, tom)

        val expected = listOf(
                Ytelse(
                        tema = "AAP",
                        fom = fom,
                        tom = tom
                ),
                Ytelse(
                        tema = "DAG",
                        fom = fom,
                        tom = tom
                ),
                Ytelse(
                        tema = "SYKEPENGER",
                        fom = fom,
                        tom = tom
                ),
                Ytelse(
                        tema = "FORELDREPENGER",
                        fom = fom,
                        tom = tom
                ),
                Ytelse(
                        tema = "ENGANGSTØNAD",
                        fom = fom,
                        tom = tom
                ),
                Ytelse(
                        tema = "PÅRØRENDESYKDOM",
                        fom = fom,
                        tom = tom
                )
        )
        val actual = ytelseService.finnYtelser(aktørId, fom, tom)

        assertTrue(actual is Either.Right)
        actual as Either.Right
        assertEquals(expected, actual.b)
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

    private fun ytelserFraInfotrygd(fom: LocalDate, tom: LocalDate) = FinnGrunnlagListeResponse().apply {
        with (foreldrepengerListe) {
            add(Foreldrepenger().apply {
                with (vedtakListe) {
                    add(Vedtak().apply {
                        anvistPeriode = Periode().apply {
                            this.fom = fom.toXmlGregorianCalendar()
                            this.tom = tom.toXmlGregorianCalendar()
                        }
                    })
                }
            })
        }
        with (sykepengerListe) {
            add(Sykepenger().apply {
                with (vedtakListe) {
                    add(Vedtak().apply {
                        anvistPeriode = Periode().apply {
                            this.fom = fom.toXmlGregorianCalendar()
                            this.tom = tom.toXmlGregorianCalendar()
                        }
                    })
                }
            })
        }
        with (engangstoenadListe) {
            add(Engangsstoenad().apply {
                with (vedtakListe) {
                    add(Vedtak().apply {
                        anvistPeriode = Periode().apply {
                            this.fom = fom.toXmlGregorianCalendar()
                            this.tom = tom.toXmlGregorianCalendar()
                        }
                    })
                }
            })
        }
        with (paaroerendeSykdomListe) {
            add(PaaroerendeSykdom().apply {
                with (vedtakListe) {
                    add(Vedtak().apply {
                        anvistPeriode = Periode().apply {
                            this.fom = fom.toXmlGregorianCalendar()
                            this.tom = tom.toXmlGregorianCalendar()
                        }
                    })
                }
            })
        }
    }.right()
}
