package no.nav.helse.oppslag.infotrygd

import arrow.core.Try
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.InfotrygdSakV1
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Periode
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.meldinger.FinnSakListeRequest
import java.time.LocalDate

class InfotrygdSakClient(private val port: InfotrygdSakV1) {

    fun finnSakListe(fødselsnummer: String, fom: LocalDate, tom: LocalDate?) =
            Try {
                port.finnSakListe(finnSakListeRequest(fødselsnummer, fom, tom))
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
