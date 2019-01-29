package no.nav.helse.ws.sakogbehandling

import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeRequest
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeResponse
import org.slf4j.LoggerFactory

class SakOgBehandlingClient(private val sakOgBehandling: SakOgBehandlingV1) {

    private val log = LoggerFactory.getLogger("SakOgBehandlingClient")

    fun finnSakOgBehandling(aktorId: String): OppslagResult {
        val request = FinnSakOgBehandlingskjedeListeRequest()
                .apply { this.aktoerREF = aktorId }
                .apply { this.isKunAapneBehandlingskjeder = true }

        return try {
            val remoteResult: FinnSakOgBehandlingskjedeListeResponse? = sakOgBehandling.finnSakOgBehandlingskjedeListe(request)
            Success(remoteResult)
        } catch (ex: Exception) {
            log.error("Error while doing sak og behndling lookup", ex)
            Failure(listOf(ex.message ?: "unknown error"))
        }
    }
}
