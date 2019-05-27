package no.nav.helse.domene.sykepengehistorikk

import arrow.core.Either
import arrow.core.right
import arrow.core.success
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.ytelse.domain.Utbetalingsvedtak
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagClient
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Behandlingstema
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Periode
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Sykepenger
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Vedtak
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykepengehistorikkServiceTest {

    @Test
    fun `skal returnere liste over perioder`() {
        val infotrygdBeregningsgrunnlagClient = mockk<InfotrygdBeregningsgrunnlagClient>()
        val aktørregisterService = mockk<AktørregisterService>()
        val sykepengehistorikkService = SykepengehistorikkService(
                infotrygdBeregningsgrunnlagClient = infotrygdBeregningsgrunnlagClient,
                aktørregisterService = aktørregisterService
        )

        val aktørId = AktørId("123456789")
        val fødselsnummer = Fødselsnummer("11111111111")
        val fom = LocalDate.now().minusMonths(3)
        val tom = LocalDate.now()

        every {
            aktørregisterService.fødselsnummerForAktør(aktørId)
        } returns fødselsnummer.value.right()

        every {
            infotrygdBeregningsgrunnlagClient.finnGrunnlagListe(fødselsnummer, fom, tom)
        } returns FinnGrunnlagListeResponse().apply {
            with (sykepengerListe) {
                add(Sykepenger().apply {
                    identdato = fom.toXmlGregorianCalendar()
                    behandlingstema = Behandlingstema().apply {
                        value = "SP"
                    }
                    with (vedtakListe) {
                        add(Vedtak().apply {
                            utbetalingsgrad = null
                            anvistPeriode = Periode().apply {
                                this.fom = fom.toXmlGregorianCalendar()
                                this.tom = tom.toXmlGregorianCalendar()
                            }
                        })
                        add(Vedtak().apply {
                            utbetalingsgrad = 0
                            anvistPeriode = Periode().apply {
                                this.fom = fom.toXmlGregorianCalendar()
                                this.tom = tom.toXmlGregorianCalendar()
                            }
                        })
                        add(Vedtak().apply {
                            utbetalingsgrad = 100
                            anvistPeriode = Periode().apply {
                                this.fom = fom.toXmlGregorianCalendar()
                                this.tom = tom.toXmlGregorianCalendar()
                            }
                        })
                    }
                })
            }
        }.success()

        val expected = Either.Right(listOf(
                Utbetalingsvedtak.SkalUtbetales(fom, tom, 100)
        ))

        assertEquals(expected, sykepengehistorikkService.hentSykepengeHistorikk(aktørId, fom, tom))
    }
}
