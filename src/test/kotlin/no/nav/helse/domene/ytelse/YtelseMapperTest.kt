package no.nav.helse.domene.ytelse

import no.nav.helse.domene.ytelse.domain.Kilde
import no.nav.helse.domene.ytelse.domain.Ytelse
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Behandlingstema
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Periode
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Sykepenger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YtelseMapperTest {

    @Test
    fun `periode-fom og periode-tom kan være null`() {
        val expected = Ytelse(
                kilde = Kilde.Infotrygd,
                tema = "SP",
                fom = null,
                tom = null
        )

        assertEquals(expected, YtelseMapper.fraInfotrygd(Sykepenger().apply {
            behandlingstema = Behandlingstema().apply {
                value = "SP"
            }
            periode = Periode()
        }))
    }
}
