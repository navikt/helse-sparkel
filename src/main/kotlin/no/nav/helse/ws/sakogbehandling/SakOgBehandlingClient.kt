package no.nav.helse.ws.sakogbehandling

import no.nav.helse.OppslagResult
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeRequest
import org.slf4j.LoggerFactory

class SakOgBehandlingClient(private val sakOgBehandling: SakOgBehandlingV1) {

    private val log = LoggerFactory.getLogger("SakOgBehandlingClient")

    fun finnSakOgBehandling(aktorId: String): OppslagResult<Exception, List<Sak>> {
        val request = FinnSakOgBehandlingskjedeListeRequest()
                .apply { this.aktoerREF = aktorId }
                .apply { this.isKunAapneBehandlingskjeder = true }

        return try {
            val saker = sakOgBehandling.finnSakOgBehandlingskjedeListe(request).sak.map(::mapSak)
            OppslagResult.Ok(saker)
        } catch (ex: Exception) {
            log.error("Error while doing sak og behndling lookup", ex)
            OppslagResult.Feil(ex)
        }
    }
}
