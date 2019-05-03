package no.nav.helse.domene.infotrygd

import arrow.core.Either
import arrow.core.Try
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagListeClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.fail
import java.time.LocalDate

class InfotrygdBeregningsgrunnlagServiceTest {

    fun `finnGrunnlagListeResponse kan være null`() {
        val infotrygdClient = mockk< InfotrygdBeregningsgrunnlagListeClient>()
        val aktørregisterService = mockk< AktørregisterService>()

        val infotrygdBeregningsgrunnlagService = InfotrygdBeregningsgrunnlagService(infotrygdClient, aktørregisterService)

        val aktørId = AktørId("123456789")
        val fødselsnummer = "987654321"
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now()

        every {
            aktørregisterService.fødselsnummerForAktør(aktørId)
        } returns Either.Right(fødselsnummer)

        every {
            infotrygdClient.finnGrunnlagListe(Fødselsnummer(fødselsnummer), tom, fom)
        } returns Try.Success(null)

        when (val actual = infotrygdBeregningsgrunnlagService.finnGrunnlagListe(aktørId, fom, tom)) {
            is Either.Left -> assertEquals(Feilårsak.FeilFraTjeneste, actual.a)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }
}
