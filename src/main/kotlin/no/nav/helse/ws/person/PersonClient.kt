package no.nav.helse.ws.person

import io.ktor.http.HttpStatusCode
import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import org.slf4j.LoggerFactory

class PersonClient(private val personV3: PersonV3) {

    private val log = LoggerFactory.getLogger("PersonClient")

    fun personInfo(id: AktørId): OppslagResult<Feil, Person> {
        val request = HentPersonRequest().apply {
            aktoer = AktoerId().apply {
                aktoerId = id.aktor
            }
            informasjonsbehov.add(Informasjonsbehov.ADRESSE)
        }

        return try {
            val tpsResponse = personV3.hentPerson(request)
            OppslagResult.Ok(PersonMapper.toPerson(tpsResponse))
        } catch (ex: Exception) {
            log.error("Error while doing person lookup", ex)
            OppslagResult.Feil(HttpStatusCode.InternalServerError, Feil.Exception(ex.message ?: "unknown error", ex))
        }
    }

    fun geografiskTilknytning(id : AktørId) : OppslagResult<Feil, GeografiskTilknytning> {
        val request = HentGeografiskTilknytningRequest().apply {
            aktoer = AktoerId().apply {
                aktoerId = id.aktor
            }
        }

        return try {
            val tpsResponse = personV3.hentGeografiskTilknytning(request)
            OppslagResult.Ok(GeografiskTilknytningMapper.tilGeografiskTilknytning(tpsResponse))
        } catch (cause: Throwable) {
            log.error("Error while doing geografisk tilknytning lookup", cause)
            OppslagResult.Feil(HttpStatusCode.InternalServerError, Feil.Exception(cause.message ?: "unknown error", cause))
        }
    }
}







