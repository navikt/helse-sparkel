package no.nav.helse.ws.person

import no.nav.helse.Feilårsak
import no.nav.helse.OppslagResult
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning

class PersonService(private val personClient: PersonClient) {

    fun personInfo(aktørId: AktørId): OppslagResult<Feilårsak, Person> {
        val lookupResult = personClient.personInfo(aktørId)
        return when (lookupResult) {
            is OppslagResult.Ok -> lookupResult
            is OppslagResult.Feil -> {
                OppslagResult.Feil(when (lookupResult.feil) {
                    is HentPersonPersonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentPersonSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.FeilFraTjeneste
                })
            }
        }
    }
    fun geografiskTilknytning(aktørId: AktørId): OppslagResult<Feilårsak, GeografiskTilknytning> {
        val lookupResult = personClient.geografiskTilknytning(aktørId)
        return when (lookupResult) {
            is OppslagResult.Ok -> lookupResult
            is OppslagResult.Feil -> {
                OppslagResult.Feil(when (lookupResult.feil) {
                    is HentGeografiskTilknytningPersonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentGeografiskTilknytningSikkerhetsbegrensing -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.FeilFraTjeneste
                })
            }
        }
    }
}
