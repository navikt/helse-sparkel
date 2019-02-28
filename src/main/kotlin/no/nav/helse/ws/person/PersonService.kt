package no.nav.helse.ws.person

import no.nav.helse.Feilårsak
import no.nav.helse.mapLeft
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning

class PersonService(private val personClient: PersonClient) {

    fun personInfo(aktørId: AktørId) =
            personClient.personInfo(aktørId).mapLeft {
                when (it) {
                    is HentPersonPersonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentPersonSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.FeilFraTjeneste
                }
            }

    fun geografiskTilknytning(aktørId: AktørId) =
            personClient.geografiskTilknytning(aktørId).mapLeft {
                when (it) {
                    is HentGeografiskTilknytningPersonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentGeografiskTilknytningSikkerhetsbegrensing -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.FeilFraTjeneste
                }
            }
}
