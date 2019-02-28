package no.nav.helse.ws.person

import no.nav.helse.Either
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import org.slf4j.LoggerFactory

class PersonClient(private val personV3: PersonV3) {

    private val log = LoggerFactory.getLogger("PersonClient")

    fun personInfo(id: AktørId): Either<Exception, Person> {
        val request = HentPersonRequest().apply {
            aktoer = AktoerId().apply {
                aktoerId = id.aktor
            }
            informasjonsbehov.add(Informasjonsbehov.ADRESSE)
        }

        return try {
            val tpsResponse = personV3.hentPerson(request)
            Either.Right(PersonMapper.toPerson(tpsResponse))
        } catch (ex: Exception) {
            log.error("Error while doing person lookup", ex)
            Either.Left(ex)
        }
    }

    fun geografiskTilknytning(id : AktørId) : Either<Exception, GeografiskTilknytning> {
        val request = HentGeografiskTilknytningRequest().apply {
            aktoer = AktoerId().apply {
                aktoerId = id.aktor
            }
        }

        return try {
            val tpsResponse = personV3.hentGeografiskTilknytning(request)
            Either.Right(GeografiskTilknytningMapper.tilGeografiskTilknytning(tpsResponse))
        } catch (err: Exception) {
            log.error("Error while doing geografisk tilknytning lookup", err)
            Either.Left(err)
        }
    }
}







