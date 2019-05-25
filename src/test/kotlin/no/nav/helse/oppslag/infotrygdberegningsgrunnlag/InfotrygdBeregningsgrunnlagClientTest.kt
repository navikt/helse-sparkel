package no.nav.helse.oppslag.infotrygdberegningsgrunnlag

import arrow.core.Try
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.domene.Fødselsnummer
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InfotrygdBeregningsgrunnlagClientTest {

    @Test
    fun `må håndtere at response kan være null`() {
        val infotrygdBeregningsgrunnlagV1 = mockk< InfotrygdBeregningsgrunnlagV1>()
        val client = InfotrygdBeregningsgrunnlagClient(infotrygdBeregningsgrunnlagV1)

        every {
            infotrygdBeregningsgrunnlagV1.finnGrunnlagListe(any())
        } answers { nothing }

        val actual = client.finnGrunnlagListe(Fødselsnummer("11111111111"), LocalDate.now(), LocalDate.now())

        actual as Try.Success

        assertNotNull(actual.value)
    }
}
