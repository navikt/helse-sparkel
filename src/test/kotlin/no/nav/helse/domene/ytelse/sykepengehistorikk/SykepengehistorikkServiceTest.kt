package no.nav.helse.domene.ytelse.sykepengehistorikk

import arrow.core.Either
import arrow.core.right
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.ytelse.domain.Behandlingstema
import no.nav.helse.domene.ytelse.domain.Beregningsgrunnlag
import no.nav.helse.domene.ytelse.domain.Utbetalingsvedtak
import no.nav.helse.domene.ytelse.infotrygd.InfotrygdService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykepengehistorikkServiceTest {

    @Test
    fun `skal returnere liste over perioder`() {
        val infotrygdService = mockk<InfotrygdService>()
        val aktørregisterService = mockk<AktørregisterService>()
        val sykepengehistorikkService = SykepengehistorikkService(
                infotrygdService = infotrygdService,
                aktørregisterService = aktørregisterService,
                spoleService = mockk()
        )

        val aktørId = AktørId("123456789")
        val fødselsnummer = Fødselsnummer("11111111111")
        val fom = LocalDate.now().minusMonths(3)
        val tom = LocalDate.now()

        every {
            aktørregisterService.fødselsnummerForAktør(aktørId)
        } returns fødselsnummer.value.right()

        every {
            infotrygdService.finnGrunnlag(fødselsnummer, fom, tom)
        } returns listOf(
                Beregningsgrunnlag.Sykepenger(
                        identdato = fom,
                        behandlingstema = Behandlingstema.Sykepenger,
                        periodeFom = null,
                        periodeTom = null,
                        vedtak = listOf(
                                Utbetalingsvedtak.SkalIkkeUtbetales(
                                        fom = fom,
                                        tom = tom
                                ),
                                Utbetalingsvedtak.SkalUtbetales(
                                        fom = fom,
                                        tom = tom,
                                        utbetalingsgrad = 100
                                )
                        )
                ),
                Beregningsgrunnlag.Foreldrepenger(
                        identdato = fom,
                        behandlingstema = Behandlingstema.ForeldrepengerMedFødsel,
                        periodeFom = null,
                        periodeTom = null,
                        vedtak = listOf(
                                Utbetalingsvedtak.SkalUtbetales(
                                        fom = fom.minusMonths(2),
                                        tom = tom.minusMonths(1),
                                        utbetalingsgrad = 100
                                )
                        )
                )
        ).right()

        val expected = Either.Right(listOf(
                Utbetalingsvedtak.SkalUtbetales(fom, tom, 100)
        ))

        assertEquals(expected, sykepengehistorikkService.hentSykepengeHistorikk(aktørId, fom, tom))
    }
}
