package no.nav.helse.http.aktør

import no.nav.helse.Feilårsak
import no.nav.helse.Either
import no.nav.helse.ws.AktørId

class AktørregisterService(private val aktørregisterClient: AktørregisterClient) {

    fun fødselsnummerForAktør(aktørId: AktørId): Either<Feilårsak, String> {
        val lookupResult = aktørregisterClient.gjeldendeNorskIdent(aktørId.aktor)
        return when (lookupResult) {
            is Either.Right -> lookupResult
            is Either.Left -> Either.Left(Feilårsak.IkkeFunnet)
        }
    }
}
