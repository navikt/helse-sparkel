package no.nav.helse.ws.person

import no.nav.helse.Feilårsak
import no.nav.helse.bimap
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning

class PersonService(private val personClient: PersonClient) {

    fun personInfo(aktørId: AktørId) =
            personClient.personInfo(aktørId).bimap({
                when (it) {
                    is HentPersonPersonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentPersonSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.FeilFraTjeneste
                }
            }, {
                PersonMapper.toPerson(it)
            })

    fun geografiskTilknytning(aktørId: AktørId) =
            personClient.geografiskTilknytning(aktørId).bimap({
                when (it) {
                    is HentGeografiskTilknytningPersonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentGeografiskTilknytningSikkerhetsbegrensing -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.FeilFraTjeneste
                }
            }, {
                GeografiskTilknytningMapper.tilGeografiskTilknytning(it)
            })
}
