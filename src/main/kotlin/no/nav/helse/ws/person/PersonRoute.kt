package no.nav.helse.ws.person

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.OppslagResult
import no.nav.helse.ws.AktørId
import java.time.LocalDate

fun Route.person(personClient: PersonClient) {

    get("api/person/{aktør}") {
        call.parameters["aktør"]?.let { aktørid ->
            val lookupResult = personClient.personInfo(AktørId(aktørid))
            when (lookupResult) {
                is OppslagResult.Ok -> call.respond(lookupResult.data)
                is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
            }
        } ?: call.respond(HttpStatusCode.BadRequest, "An aktørid must be specified")
    }

    get("api/person/{aktør}/history") {
        call.parameters["aktør"]?.let { aktørid ->
            val lookupResult = personClient.personHistorikk(AktørId(aktørid), LocalDate.now().minusYears(3), LocalDate.now())
            when (lookupResult) {
                is OppslagResult.Ok -> call.respond(lookupResult.data)
                is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
            }
        } ?: call.respond(HttpStatusCode.BadRequest, "An aktørid must be specified")
    }

    get("api/person/{aktør}/geografisk-tilknytning") {
        call.parameters["aktør"]?.let { aktoerId ->
            val lookupResult = personClient.geografiskTilknytning(AktørId(aktoerId))
            when (lookupResult) {
                is OppslagResult.Ok -> when {
                    lookupResult.data.erKode6() -> call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Ikke tilgang til å se geografisk tilknytning til denne aktøren."))
                    lookupResult.data.harGeografisOmraade() -> call.respond(lookupResult.data.geografiskOmraade!!)
                    else -> call.respond(HttpStatusCode.NotFound, mapOf("error" to "Aktøren har ingen geografisk tilknytning."))
                }
                is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
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
