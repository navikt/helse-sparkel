package no.nav.helse.oppslag.infotrygdberegningsgrunnlag

import arrow.core.Try
import arrow.core.failure
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeRequest
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeResponse
import org.slf4j.LoggerFactory
import java.time.LocalDate
import javax.xml.ws.soap.SOAPFaultException

class InfotrygdBeregningsgrunnlagClient(private val infotrygdBeregningsgrunnlag : InfotrygdBeregningsgrunnlagV1) {

    companion object {
        private val baseneErUtilgjengelige = "Basene i Infotrygd er ikke tilgjengelige"
        private val log = LoggerFactory.getLogger(InfotrygdBeregningsgrunnlagClient::class.java)
    }
    fun finnGrunnlagListe(fnr: String, fraOgMed: LocalDate, tilOgMed: LocalDate) =
            Try {
                infotrygdBeregningsgrunnlag.finnGrunnlagListe(createFinnGrunnlagListeRequest(fnr, fraOgMed, tilOgMed)) ?:
                        FinnGrunnlagListeResponse()
            }.let {
                if (it is Try.Failure) {
                    if (it.exception is SOAPFaultException) {
                        log.info("a soap fault was catched (is ${it.exception.javaClass}). message is <${it.exception.message}>. " +
                                "soap fault message is <${(it.exception as SOAPFaultException).fault.faultString}>", it.exception)
                        if ((it.exception as SOAPFaultException).message == baseneErUtilgjengelige) {
                            return@let BaseneErUtilgjengeligeException(it.exception as SOAPFaultException)
                                    .failure()
                        }
                    } else {
                        log.info("a exception was catched, not soap (is ${it.exception.javaClass}). message is <${it.exception.message}>", it.exception)
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
