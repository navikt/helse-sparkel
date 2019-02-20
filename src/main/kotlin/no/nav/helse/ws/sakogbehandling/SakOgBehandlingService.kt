package no.nav.helse.ws.sakogbehandling

import no.nav.helse.ws.AktørId

class SakOgBehandlingService(private val sakOgBehandlingClient: SakOgBehandlingClient) {

    fun finnSakOgBehandling(aktørId: AktørId) = sakOgBehandlingClient.finnSakOgBehandling(aktørId.aktor)
}
