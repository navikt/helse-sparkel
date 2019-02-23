package no.nav.helse.ws.sakogbehandling

import io.ktor.http.*
import no.nav.helse.*
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.*
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.*
import org.slf4j.*

class SakOgBehandlingClient(private val sakOgBehandling: SakOgBehandlingV1) {

    private val log = LoggerFactory.getLogger("SakOgBehandlingClient")

    fun finnSakOgBehandling(aktorId: String): OppslagResult<Feil, List<Sak>> {
        val request = FinnSakOgBehandlingskjedeListeRequest()
                .apply { this.aktoerREF = aktorId }
                .apply { this.isKunAapneBehandlingskjeder = true }

        return try {
            val saker = sakOgBehandling.finnSakOgBehandlingskjedeListe(request).sak.map(::mapSak)
            OppslagResult.Ok(saker)
        } catch (ex: Exception) {
            log.error("Error while doing sak og behndling lookup", ex)
            OppslagResult.Feil(HttpStatusCode.InternalServerError, Feil.Exception(ex.message ?: "unknown error", ex))
        }
    }
}
