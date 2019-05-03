package no.nav.helse.domene.sykepengehistorikk

import arrow.core.Either
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.infotrygd.InfotrygdBeregningsgrunnlagService
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
        val infotrygdService = mockk< InfotrygdBeregningsgrunnlagService>()
        val sykepengehistorikkService = SykepengehistorikkService(infotrygdService)

        val aktørId = AktørId("123456789")
        val fom = LocalDate.now().minusMonths(3)
        val tom = LocalDate.now()

        every {
            infotrygdService.finnGrunnlagListe(aktørId, fom, tom)
        } returns Either.Right(FinnGrunnlagListeResponse().apply {
            with (sykepengerListe) {
                add(Sykepenger().apply {
                    with (vedtakListe) {
                        add(Vedtak().apply {
                            utbetalingsgrad = null
                        })
                        add(Vedtak().apply {
                            utbetalingsgrad = 0
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
        })

        val expected = Either.Right(listOf(
                Tidsperiode(fom, tom)
        ))

        assertEquals(expected, sykepengehistorikkService.hentSykepengeHistorikk(aktørId, fom, tom))
    }
}
