package no.nav.helse.domene.infotrygd

import arrow.core.Either
import arrow.core.Try
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagListeClient
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InfotrygdBeregningsgrunnlagServiceTest {

    @Test
    fun `finnGrunnlagListeResponse kan være null`() {
        val infotrygdBeregningsgrunnlagListeClient = mockk<InfotrygdBeregningsgrunnlagListeClient>()
        val aktørregisterService = mockk< AktørregisterService>()

        val infotrygdBeregningsgrunnlagService = InfotrygdBeregningsgrunnlagService(infotrygdBeregningsgrunnlagListeClient, aktørregisterService)

        val aktørId = AktørId("123456789")
        val fødselsnummer = "11111111111"
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now()

        every {
            aktørregisterService.fødselsnummerForAktør(aktørId)
        } returns Either.Right(fødselsnummer)

        every {
            infotrygdBeregningsgrunnlagListeClient.finnGrunnlagListe(Fødselsnummer(fødselsnummer), fom, tom)
        } returns Try.Success(null)

        assertTrue(infotrygdBeregningsgrunnlagService.finnGrunnlagListe(aktørId, fom, tom) is Either.Right)
    }
}
