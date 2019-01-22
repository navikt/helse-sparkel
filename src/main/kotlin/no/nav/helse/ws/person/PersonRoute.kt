package no.nav.helse.ws.person

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.*
import no.nav.helse.ws.*

private const val aktørParam = "aktor"

fun Route.person(factory: () -> PersonClient) {
    val personClient: PersonClient by lazy(factory)

    get("api/person/{aktør}") {
        call.parameters["aktør"]?.let { aktørid ->
            val lookupResult: OppslagResult = personClient.personInfo(AktørId(aktørid))
            when (lookupResult) {
                is Success<*> -> call.respond(lookupResult.data!!)
                is Failure -> call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "that didn't go so well..."))
            }
        } ?: call.respond(HttpStatusCode.NotFound)

    }
}

