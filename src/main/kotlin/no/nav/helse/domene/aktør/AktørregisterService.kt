package no.nav.helse.domene.aktør

import no.nav.helse.Feilårsak
import no.nav.helse.oppslag.aktør.AktørregisterClient
import no.nav.helse.domene.AktørId

class AktørregisterService(private val aktørregisterClient: AktørregisterClient) {

    fun fødselsnummerForAktør(aktørId: AktørId) =
            aktørregisterClient.gjeldendeNorskIdent(aktørId.aktor).mapLeft {
                Feilårsak.IkkeFunnet
            }
}
