package no.nav.helse.ws.sakogbehandling

import no.nav.helse.Feilårsak
import no.nav.helse.Either
import no.nav.helse.ws.AktørId

class SakOgBehandlingService(private val sakOgBehandlingClient: SakOgBehandlingClient) {

    fun finnSakOgBehandling(aktørId: AktørId): Either<Feilårsak, List<Sak>> {
        val lookupResult = sakOgBehandlingClient.finnSakOgBehandling(aktørId.aktor)
        return when (lookupResult) {
            is Either.Right -> lookupResult
            is Either.Left -> Either.Left(Feilårsak.UkjentFeil)
        }
    }
}
