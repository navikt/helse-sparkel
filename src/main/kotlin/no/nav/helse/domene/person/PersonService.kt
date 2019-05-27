package no.nav.helse.domene.person

import arrow.core.Either
import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
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
            personClient.personMedAdresse(aktørId).toEither { err ->
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
            familierelasjoner
                    .filter { "BARN" == it.tilRolle.value }
                    .filter { aktiveEndringstyper.contains(it.endringstype) }
                    .map { AktørId((it.tilPerson.aktoer as AktoerId).aktoerId) }
        }.map { barnAktørIder ->
            barnAktørIder.map { barnAktørId ->
                personClient.person(barnAktørId).toEither { err ->
                    log.error("Feil ved oppslag på barn", err)
                    when (err) {
                        is HentGeografiskTilknytningPersonIkkeFunnet -> Feilårsak.IkkeFunnet
                        is HentGeografiskTilknytningSikkerhetsbegrensing -> Feilårsak.FeilFraTjeneste
                        else -> Feilårsak.FeilFraTjeneste
                    }
                }.map {
                    PersonMapper.toBarn(it)
                }
            }.mapNotNull {
                when (it) {
                    is Either.Right -> it.b
                    else -> null
                }
            }
        }
}
