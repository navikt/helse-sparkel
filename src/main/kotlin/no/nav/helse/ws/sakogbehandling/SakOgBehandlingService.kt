package no.nav.helse.ws.sakogbehandling

import no.nav.helse.Feilårsak
import no.nav.helse.ws.AktørId
import org.slf4j.LoggerFactory

class SakOgBehandlingService(private val sakOgBehandlingClient: SakOgBehandlingClient) {

    companion object {
        private val log = LoggerFactory.getLogger(SakOgBehandlingService::class.java)
    }

    fun finnSakOgBehandling(aktørId: AktørId) =
            sakOgBehandlingClient.finnSakOgBehandling(aktørId.aktor).toEither { err ->
                log.error("Error while doing sak og behandling lookup", err)

                Feilårsak.UkjentFeil
            }.map {
                it.map(::mapSak)
            }
}
