package no.nav.helse.ws.sakogbehandling

import arrow.core.Try
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeRequest

class SakOgBehandlingClient(private val sakOgBehandling: SakOgBehandlingV1) {

    fun finnSakOgBehandling(aktorId: String) =
            Try {
                sakOgBehandling.finnSakOgBehandlingskjedeListe(finnSakOgBehandlingskjedeListeRequest(aktorId)).sak
            }

    private fun finnSakOgBehandlingskjedeListeRequest(aktorId: String) =
            FinnSakOgBehandlingskjedeListeRequest().apply {
                aktoerREF = aktorId
                isKunAapneBehandlingskjeder = true
            }
}
