package no.nav.helse.oppslag.infotrygd

import arrow.core.Try
import arrow.core.failure
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.BaseneErUtilgjengeligeException
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.InfotrygdSakV1
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Periode
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.meldinger.FinnSakListeRequest
import java.time.LocalDate
import javax.xml.ws.soap.SOAPFaultException

class InfotrygdSakClient(private val port: InfotrygdSakV1) {

    companion object {
        private val baseneErUtilgjengelige = "Basene i Infotrygd er ikke tilgjengelige"
    }

    fun finnSakListe(fødselsnummer: String, fom: LocalDate, tom: LocalDate?) =
            Try {
                port.finnSakListe(finnSakListeRequest(fødselsnummer, fom, tom))
            }.let {
                if (it is Try.Failure && it.exception is SOAPFaultException) {
                    if ((it.exception as SOAPFaultException).message!!.contains(baseneErUtilgjengelige)) {
                        return@let BaseneErUtilgjengeligeException(it.exception as SOAPFaultException)
                                .failure()
                    }
                }
                it
            }

    private fun finnSakListeRequest(fødselsnummer: String, fom: LocalDate, tom: LocalDate?) =
            FinnSakListeRequest().apply {
                personident = fødselsnummer
                periode = Periode().apply {
                    this.fom = fom.toXmlGregorianCalendar()
                    this.tom = tom?.toXmlGregorianCalendar()
                }
            }
}
