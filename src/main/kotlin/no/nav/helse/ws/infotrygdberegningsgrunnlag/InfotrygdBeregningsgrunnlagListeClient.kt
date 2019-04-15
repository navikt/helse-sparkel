package no.nav.helse.ws.infotrygdberegningsgrunnlag

import arrow.core.Try
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeRequest
import java.time.LocalDate

class InfotrygdBeregningsgrunnlagListeClient(private val infotrygdBeregningsgrunnlag : InfotrygdBeregningsgrunnlagV1) {

    fun finnGrunnlagListe(fnr: Fødselsnummer, fraOgMed: LocalDate, tilOgMed: LocalDate) =
            Try {
                infotrygdBeregningsgrunnlag.finnGrunnlagListe(createFinnGrunnlagListeRequest(fnr.value, fraOgMed, tilOgMed))
            }

    private fun createFinnGrunnlagListeRequest(fnr: String, fraOgMed: LocalDate, tilOgMed: LocalDate): FinnGrunnlagListeRequest {
        return FinnGrunnlagListeRequest()
                .apply {
                    personident = fnr
                    fom = fraOgMed.toXmlGregorianCalendar()
                    tom = tilOgMed.toXmlGregorianCalendar()
                }
    }

}
