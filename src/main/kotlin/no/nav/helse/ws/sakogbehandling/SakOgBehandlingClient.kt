package no.nav.helse.ws.sakogbehandling

import no.nav.helse.Either
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.finnsakogbehandlingskjedeliste.Sak
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeRequest
import org.slf4j.LoggerFactory

class SakOgBehandlingClient(private val sakOgBehandling: SakOgBehandlingV1) {

    private val log = LoggerFactory.getLogger("SakOgBehandlingClient")

    fun finnSakOgBehandling(aktorId: String): Either<Exception, List<Sak>> {
        val request = FinnSakOgBehandlingskjedeListeRequest().apply {
            aktoerREF = aktorId
            isKunAapneBehandlingskjeder = true
        }

        return try {
            Either.Right(sakOgBehandling.finnSakOgBehandlingskjedeListe(request).sak)
        } catch (ex: Exception) {
            log.error("Error while doing sak og behandling lookup", ex)
            Either.Left(ex)
        }
    }
}
