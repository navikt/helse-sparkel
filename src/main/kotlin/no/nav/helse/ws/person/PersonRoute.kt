package no.nav.helse.ws.person

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.HttpFeil
import no.nav.helse.Either
import no.nav.helse.respondFeil
import no.nav.helse.ws.AktørId

fun Route.person(personService: PersonService) {

    get("api/person/{aktør}") {
        call.parameters["aktør"]?.let { aktørid ->
            val lookupResult = personService.personInfo(AktørId(aktørid))
            when (lookupResult) {
                is Either.Right -> call.respond(lookupResult.right)
                is Either.Left -> call.respondFeil(lookupResult.left)
            }
        } ?: call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "An aktørid must be specified"))
    }

    get("api/person/{aktør}/geografisk-tilknytning") {
        call.parameters["aktør"]?.let { aktoerId ->
            val lookupResult = personService.geografiskTilknytning(AktørId(aktoerId))
            when (lookupResult) {
                is Either.Right -> when {
                    lookupResult.right.erKode6() -> call.respondFeil(HttpFeil(HttpStatusCode.Forbidden, "Ikke tilgang til å se geografisk tilknytning til denne aktøren."))
                    lookupResult.right.harGeografisOmraade() -> call.respond(lookupResult.right.geografiskOmraade!!)
                    else -> call.respondFeil(HttpFeil(HttpStatusCode.NotFound, "Aktøren har ingen geografisk tilknytning."))
                }
                is Either.Left -> call.respondFeil(lookupResult.left)
            }
        } ?: call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "En Aktør ID må oppgis."))
    }
}

private fun GeografiskTilknytning.erKode6(): Boolean {
    return diskresjonskode?.kode == 6
}

private fun GeografiskTilknytning.harGeografisOmraade() : Boolean {
    return geografiskOmraade != null
}
