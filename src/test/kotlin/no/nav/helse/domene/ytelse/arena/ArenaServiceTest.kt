package no.nav.helse.domene.ytelse.arena

import arrow.core.Either
import arrow.core.success
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.ytelse.domain.Kilde
import no.nav.helse.domene.ytelse.domain.Ytelse
import no.nav.helse.oppslag.arena.MeldekortUtbetalingsgrunnlagClient
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Sak
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Tema
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.meldinger.FinnMeldekortUtbetalingsgrunnlagListeResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArenaServiceTest {

    @Test
    fun `skal hente saker`() {

        val meldekortUtbetalingsgrunnlagClient = mockk<MeldekortUtbetalingsgrunnlagClient>()

        val arenaService = ArenaService(meldekortUtbetalingsgrunnlagClient)

        val aktørId = AktørId("123456789")
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now()

        every {
            meldekortUtbetalingsgrunnlagClient.finnMeldekortUtbetalingsgrunnlag(aktørId, fom, tom)
        } returns ytelserFraArena(fom, tom)

        val expected = listOf(
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

        val actual = arenaService.finnSaker(aktørId, fom, tom)

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

}
