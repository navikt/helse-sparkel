package no.nav.helse.oppslag.infotrygdberegningsgrunnlag

import arrow.core.Try
import arrow.core.failure
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeRequest
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeResponse
import java.time.LocalDate
import javax.xml.ws.soap.SOAPFaultException

class InfotrygdBeregningsgrunnlagClient(private val infotrygdBeregningsgrunnlag : InfotrygdBeregningsgrunnlagV1) {

    companion object {
        private val baseneErUtilgjengelige = "Basene i Infotrygd er ikke tilgjengelige"
    }
    fun finnGrunnlagListe(fnr: String, fraOgMed: LocalDate, tilOgMed: LocalDate) =
            Try {
                infotrygdBeregningsgrunnlag.finnGrunnlagListe(createFinnGrunnlagListeRequest(fnr, fraOgMed, tilOgMed)) ?:
                        FinnGrunnlagListeResponse()
            }.let {
                if (it is Try.Failure && it.exception is SOAPFaultException) {
                    if ((it.exception as SOAPFaultException).message!!.contains(baseneErUtilgjengelige)) {
                        return@let BaseneErUtilgjengeligeException(it.exception as SOAPFaultException)
                                .failure()
                    }
                }
                it
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
