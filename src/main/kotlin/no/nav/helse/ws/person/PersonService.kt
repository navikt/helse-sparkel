package no.nav.helse.ws.person

import no.nav.helse.Feilårsak
import no.nav.helse.Either
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning

class PersonService(private val personClient: PersonClient) {

    fun personInfo(aktørId: AktørId): Either<Feilårsak, Person> {
        val lookupResult = personClient.personInfo(aktørId)
        return when (lookupResult) {
            is Either.Right -> lookupResult
            is Either.Left -> {
                Either.Left(when (lookupResult.left) {
                    is HentPersonPersonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentPersonSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.FeilFraTjeneste
                })
            }
        }
    }
    fun geografiskTilknytning(aktørId: AktørId): Either<Feilårsak, GeografiskTilknytning> {
        val lookupResult = personClient.geografiskTilknytning(aktørId)
        return when (lookupResult) {
            is Either.Right -> lookupResult
            is Either.Left -> {
                Either.Left(when (lookupResult.left) {
                    is HentGeografiskTilknytningPersonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentGeografiskTilknytningSikkerhetsbegrensing -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.FeilFraTjeneste
                })
            }
        }
    }
}
