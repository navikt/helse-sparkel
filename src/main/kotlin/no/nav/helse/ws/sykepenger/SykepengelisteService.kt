package no.nav.helse.ws.sykepenger

import no.nav.helse.Feilårsak
import no.nav.helse.OppslagResult
import no.nav.helse.flatMap
import no.nav.helse.http.aktør.AktørregisterService
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.sykepenger.v2.binding.HentSykepengerListeSikkerhetsbegrensning
import java.time.LocalDate

class SykepengelisteService(private val sykepengerClient: SykepengerClient, private val aktørregisterService: AktørregisterService) {

    fun finnSykepengevedtak(aktørId: AktørId, fom: LocalDate, tom: LocalDate): OppslagResult<Feilårsak, Collection<SykepengerVedtak>> {
        return aktørregisterService.fødselsnummerForAktør(aktørId).flatMap { fnr ->
            val lookupResult = sykepengerClient.finnSykepengeVedtak(Fødselsnummer(fnr), fom, tom)
            when (lookupResult) {
                is OppslagResult.Ok -> lookupResult
                is OppslagResult.Feil -> {
                    OppslagResult.Feil(when (lookupResult.feil) {
                        is HentSykepengerListeSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                        else -> Feilårsak.UkjentFeil
                    })
                }
            }
        }
    }

}
