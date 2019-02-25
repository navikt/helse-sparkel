package no.nav.helse.http.aktør

import no.nav.helse.Feilårsak
import no.nav.helse.OppslagResult
import no.nav.helse.ws.AktørId

class AktørregisterService(private val aktørregisterClient: AktørregisterClient) {

    fun fødselsnummerForAktør(aktørId: AktørId): OppslagResult<Feilårsak, String> {
        val lookupResult = aktørregisterClient.gjeldendeNorskIdent(aktørId.aktor)
        return when (lookupResult) {
            is OppslagResult.Ok -> lookupResult
            is OppslagResult.Feil -> OppslagResult.Feil(Feilårsak.IkkeFunnet)
        }
    }
}
