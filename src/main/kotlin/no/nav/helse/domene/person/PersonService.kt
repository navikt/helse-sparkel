package no.nav.helse.domene.person

import arrow.core.Either
import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.person.domain.Person
import no.nav.helse.oppslag.person.PersonClient
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.person.v3.metadata.Endringstyper
import org.slf4j.LoggerFactory

class PersonService(private val personClient: PersonClient) {

    companion object {
        private val aktiveEndringstyper = listOf(Endringstyper.NY, Endringstyper.ENDRET, null)
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

    fun barn(aktørId: AktørId) =
        personClient.familierelasjoner(aktørId).toEither { error ->
            log.error("Feil ved oppslag på familierelasjoner", error)
            when (error) {
                is HentPersonPersonIkkeFunnet -> Feilårsak.IkkeFunnet
                is HentPersonSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                else -> Feilårsak.FeilFraTjeneste
            }
        }.map { familierelasjoner ->
            /*
                "tilPerson" som blir returnert som en del av Familierelasjon inneholder kun aktørId og navn.
                Må gjøre oppslag på hvert enkelt barn for å kunne avgjøre status og disresjonskode
             */
            val barnAktørIder =  familierelasjoner
                    .filter { "BARN" == it.tilRolle.value }
                    .filter { aktiveEndringstyper.contains(it.endringstype) }
                    .map { AktørId((it.tilPerson.aktoer as AktoerId).aktoerId) }

            log.trace("Slår opp info på '${barnAktørIder.size}' barn.")

            val barn = mutableListOf<Person>()
            barnAktørIder.forEach { barnAktørId ->
                val oppslagResult = personInfo(barnAktørId)
                when (oppslagResult) {
                    is Either.Right -> barn.add(oppslagResult.b)
                    is Either.Left -> log.error("Feil ved oppslag på barn med AktørID '$barnAktørId'")
                }
            }
            barn.toList()
        }
}
