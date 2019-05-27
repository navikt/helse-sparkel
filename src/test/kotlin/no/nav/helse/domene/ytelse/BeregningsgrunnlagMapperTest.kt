package no.nav.helse.domene.ytelse

import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.ytelse.BeregningsgrunnlagMapper.toBeregningsgrunnlag
import no.nav.helse.domene.ytelse.BeregningsgrunnlagMapper.toVedtak
import no.nav.helse.domene.ytelse.domain.Behandlingstema
import no.nav.helse.domene.ytelse.domain.Beregningsgrunnlag
import no.nav.helse.domene.ytelse.domain.Utbetalingsvedtak
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate



class BeregningsgrunnlagMapperTest {

    @Test
    fun `skal mappe sykepenger`() {
        val identdato = LocalDate.now().minusMonths(1)
        val fom = LocalDate.now().minusDays(14)
        val tom = LocalDate.now()
        val behandlingstema = "SP"

        val given = Sykepenger().apply {
            this.identdato = identdato.toXmlGregorianCalendar()
            periode = Periode().apply {
                this.fom = fom.toXmlGregorianCalendar()
                this.tom = tom.toXmlGregorianCalendar()
            }
            this.behandlingstema = no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Behandlingstema().apply {
                value = behandlingstema
            }
        }

        val expected = Beregningsgrunnlag.Sykepenger(
                identdato = identdato,
                periodeFom = fom,
                periodeTom = tom,
                behandlingstema = Behandlingstema.Sykepenger,
                vedtak = emptyList()
        )

        assertEquals(expected, toBeregningsgrunnlag(given))
    }

    @Test
    fun `skal mappe foreldrepenger`() {
        val identdato = LocalDate.now().minusMonths(1)
        val fom = LocalDate.now().minusDays(14)
        val tom = LocalDate.now()
        val behandlingstema = "FØ"

        val given = Foreldrepenger().apply {
            this.identdato = identdato.toXmlGregorianCalendar()
            periode = Periode().apply {
                this.fom = fom.toXmlGregorianCalendar()
                this.tom = tom.toXmlGregorianCalendar()
            }
            this.behandlingstema = no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Behandlingstema().apply {
                value = behandlingstema
            }
        }

        val expected = Beregningsgrunnlag.Foreldrepenger(
                identdato = identdato,
                periodeFom = fom,
                periodeTom = tom,
                behandlingstema = Behandlingstema.ForeldrepengerMedFødsel,
                vedtak = emptyList()
        )

        assertEquals(expected, toBeregningsgrunnlag(given))
    }

    @Test
    fun `skal mappe engangstønad`() {
        val identdato = LocalDate.now().minusMonths(1)
        val fom = LocalDate.now().minusDays(14)
        val tom = LocalDate.now()
        val behandlingstema = "AE"

        val given = Engangsstoenad().apply {
            this.identdato = identdato.toXmlGregorianCalendar()
            periode = Periode().apply {
                this.fom = fom.toXmlGregorianCalendar()
                this.tom = tom.toXmlGregorianCalendar()
            }
            this.behandlingstema = no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Behandlingstema().apply {
                value = behandlingstema
            }
        }

        val expected = Beregningsgrunnlag.Engangstønad(
                identdato = identdato,
                periodeFom = fom,
                periodeTom = tom,
                behandlingstema = Behandlingstema.EngangstønadMedAdopsjon,
                vedtak = emptyList()
        )

        assertEquals(expected, toBeregningsgrunnlag(given))
    }

    @Test
    fun `skal mappe pårørende sykdom`() {
        val identdato = LocalDate.now().minusMonths(1)
        val fom = LocalDate.now().minusDays(14)
        val tom = LocalDate.now()
        val behandlingstema = "PN"

        val given = PaaroerendeSykdom().apply {
            this.identdato = identdato.toXmlGregorianCalendar()
            periode = Periode().apply {
                this.fom = fom.toXmlGregorianCalendar()
                this.tom = tom.toXmlGregorianCalendar()
            }
            this.behandlingstema = no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Behandlingstema().apply {
                value = behandlingstema
            }
        }

        val expected = Beregningsgrunnlag.PårørendeSykdom(
                identdato = identdato,
                periodeFom = fom,
                periodeTom = tom,
                behandlingstema = Behandlingstema.Pleiepenger,
                vedtak = emptyList()
        )

        assertEquals(expected, toBeregningsgrunnlag(given))
    }

    @Test
    fun `skal mappe vedtak`() {
        val fom = LocalDate.now().minusDays(14)
        val tom = LocalDate.now()
        val utbetalingsgrad = 100


        val given = listOf(
                Vedtak().apply {
                    anvistPeriode = Periode().apply {
                        this.fom = fom.toXmlGregorianCalendar()
                        this.tom = tom.toXmlGregorianCalendar()
                    }
                    this.utbetalingsgrad = utbetalingsgrad
                }
        )

        val expected = listOf(
                Utbetalingsvedtak.SkalUtbetales(
                        fom = fom,
                        tom = tom,
                        utbetalingsgrad = utbetalingsgrad
                ))

        assertEquals(expected, toVedtak(given))
    }

    @Test
    fun `skal mappe vedtak uten utbetalingsgrad`() {
        val fom = LocalDate.now().minusDays(14)
        val tom = LocalDate.now()
        val utbetalingsgrad = 100


        val given = listOf(
                Vedtak().apply {
                    anvistPeriode = Periode().apply {
                        this.fom = fom.toXmlGregorianCalendar()
                        this.tom = tom.toXmlGregorianCalendar()
                    }
                }
        )

        val expected = listOf(
                Utbetalingsvedtak.SkalIkkeUtbetales(
                        fom = fom,
                        tom = tom
                ))

        assertEquals(expected, toVedtak(given))
    }
}
