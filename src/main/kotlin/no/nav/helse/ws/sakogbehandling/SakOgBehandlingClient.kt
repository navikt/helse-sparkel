package no.nav.helse.ws.sakogbehandling

import io.ktor.http.HttpStatusCode
import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeRequest
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeResponse
import org.slf4j.LoggerFactory

class SakOgBehandlingClient(private val sakOgBehandling: SakOgBehandlingV1) {

    private val log = LoggerFactory.getLogger("SakOgBehandlingClient")

    fun finnSakOgBehandling(aktorId: String): OppslagResult<Feil, FinnSakOgBehandlingskjedeListeResponse> {
        val request = FinnSakOgBehandlingskjedeListeRequest()
                .apply { this.aktoerREF = aktorId }
                .apply { this.isKunAapneBehandlingskjeder = true }

        return try {
            OppslagResult.Ok(sakOgBehandling.finnSakOgBehandlingskjedeListe(request))
        } catch (ex: Exception) {
            log.error("Error while doing sak og behndling lookup", ex)
            OppslagResult.Feil(HttpStatusCode.InternalServerError, Feil.Exception(ex.message ?: "unknown error", ex))
        }
    }
}
