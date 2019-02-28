package no.nav.helse.http.aktør

import no.nav.helse.Feilårsak
import no.nav.helse.mapLeft
import no.nav.helse.ws.AktørId

class AktørregisterService(private val aktørregisterClient: AktørregisterClient) {

    fun fødselsnummerForAktør(aktørId: AktørId) =
            aktørregisterClient.gjeldendeNorskIdent(aktørId.aktor).mapLeft {
                Feilårsak.IkkeFunnet
            }
}
