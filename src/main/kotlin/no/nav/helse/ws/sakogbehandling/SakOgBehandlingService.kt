package no.nav.helse.ws.sakogbehandling

import no.nav.helse.Feilårsak
import no.nav.helse.OppslagResult
import no.nav.helse.ws.AktørId

class SakOgBehandlingService(private val sakOgBehandlingClient: SakOgBehandlingClient) {

    fun finnSakOgBehandling(aktørId: AktørId): OppslagResult<Feilårsak, List<Sak>> {
        val lookupResult = sakOgBehandlingClient.finnSakOgBehandling(aktørId.aktor)
        return when (lookupResult) {
            is OppslagResult.Ok -> lookupResult
            is OppslagResult.Feil -> OppslagResult.Feil(Feilårsak.UkjentFeil)
        }
    }
}
