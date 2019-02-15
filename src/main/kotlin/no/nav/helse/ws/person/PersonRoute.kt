package no.nav.helse.ws.person

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.*
import no.nav.helse.ws.*
import java.time.*

fun Route.person(factory: () -> PersonClient) {
    val personClient: PersonClient by lazy(factory)

    get("api/person/{aktør}") {
        call.parameters["aktør"]?.let { aktørid ->
            val lookupResult: OppslagResult = personClient.personInfo(AktørId(aktørid))
            when (lookupResult) {
                is Success<*> -> call.respond(lookupResult.data!!)
                is Failure -> call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "that didn't go so well..."))
            }
        } ?: call.respond(HttpStatusCode.BadRequest, "An aktørid must be specified")
    }

    get("api/person/{aktør}/history") {
        call.parameters["aktør"]?.let { aktørid ->
            val lookupResult: OppslagResult =
                    personClient.personHistorikk(AktørId(aktørid), LocalDate.now().minusYears(3), LocalDate.now())
            when (lookupResult) {
                is Success<*> -> call.respond(lookupResult.data!!)
                is Failure -> call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "that didn't go so well..."))
            }
        } ?: call.respond(HttpStatusCode.BadRequest, "An aktørid must be specified")
    }

    get("api/person/{aktør}/geografisk-tilknytning") {
        call.parameters["aktør"]?.let { aktoerId ->
            val lookupResult: OppslagResult = personClient.geografiskTilknytning(AktørId(aktoerId))
            when (lookupResult) {
                is Success<*> -> {
                    val geografiskTilknytning = lookupResult.data as GeografiskTilknytning
                    when {
                        geografiskTilknytning.erKode6() -> call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Ikke tilgang til å se geografisk tilknytning til denne aktøren."))
                        geografiskTilknytning.harGeografisOmraade() -> call.respond(geografiskTilknytning.geografiskOmraade!!)
                        else -> call.respond(HttpStatusCode.NotFound, mapOf("error" to "Aktøren har ingen geografisk tilknytning."))
                    }
                }
                is Failure -> call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Feil ved henting av geografisk tilknytning."))
            }
        } ?: call.respond(HttpStatusCode.BadRequest, "En Aktør ID må oppgis.")
    }
}

private fun GeografiskTilknytning.erKode6(): Boolean {
    return diskresjonskode?.kode == 6
}

private fun GeografiskTilknytning.harGeografisOmraade() : Boolean {
    return geografiskOmraade != null
}
