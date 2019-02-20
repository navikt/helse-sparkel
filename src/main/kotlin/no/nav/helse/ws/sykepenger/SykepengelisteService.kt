package no.nav.helse.ws.sykepenger

import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.http.aktør.AktørregisterService
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.Fødselsnummer
import java.time.LocalDate

class SykepengelisteService(private val sykepengerClient: SykepengerClient, private val aktørregisterService: AktørregisterService) {

    fun finnSykepengevedtak(aktørId: AktørId, fom: LocalDate, tom: LocalDate): OppslagResult<Feil, Collection<SykepengerVedtak>> {
        val fnr = Fødselsnummer(aktørregisterService.fødselsnummerForAktør(aktørId))
        return sykepengerClient.finnSykepengeVedtak(fnr, fom, tom)
    }

}
