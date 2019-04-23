package no.nav.helse.domene.person

import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.oppslag.person.PersonClient
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning
import org.slf4j.LoggerFactory

class PersonService(private val personClient: PersonClient) {

    companion object {
        private val log = LoggerFactory.getLogger(PersonService::class.java)
    }

    fun personInfo(aktørId: AktørId) =
            personClient.personInfo(aktørId).toEither { err ->
                log.error("Error while doing person lookup", err)

                when (err) {
                    is HentPersonPersonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentPersonSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.FeilFraTjeneste
                }
            }.map {
                PersonMapper.toPerson(it)
            }

    fun geografiskTilknytning(aktørId: AktørId) =
            personClient.geografiskTilknytning(aktørId).toEither { err ->
                log.error("Error while doing geografisk tilknytning lookup", err)

                when (err) {
                    is HentGeografiskTilknytningPersonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentGeografiskTilknytningSikkerhetsbegrensing -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.FeilFraTjeneste
                }
            }.map {
                GeografiskTilknytningMapper.tilGeografiskTilknytning(it)
            }
}
