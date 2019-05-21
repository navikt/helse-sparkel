package no.nav.helse.domene.aktør

import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.oppslag.aktør.AktørregisterClient

class AktørregisterService(private val aktørregisterClient: AktørregisterClient) {

    fun fødselsnummerForAktør(aktørId: AktørId) =
            aktørregisterClient.gjeldendeNorskIdent(aktørId.aktor).mapLeft {
                Feilårsak.IkkeFunnet
            }
    fun aktørForFødselsnummer(fødselsnummer: Fødselsnummer) =
            aktørregisterClient.gjeldendeAktørId(fødselsnummer.value).mapLeft {
                Feilårsak.IkkeFunnet
            }
}
