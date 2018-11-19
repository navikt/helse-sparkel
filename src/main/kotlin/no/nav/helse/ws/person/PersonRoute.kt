package no.nav.helse.ws.person

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.*
import no.nav.helse.ws.*

fun Routing.person(personClient: PersonClient) {
    post("api/person") {
        call.receiveParameters()["fnr"]?.let { fnr ->
            val lookupResult: OppslagResult = personClient.personInfo(Fødselsnummer(fnr))
            when (lookupResult) {
                is Success<*> -> call.respond(lookupResult.data!!)
                is Failure -> call.respond(HttpStatusCode.InternalServerError, "that didn't go so well...")
            }
        } ?: call.respond(HttpStatusCode.BadRequest, "you need to supply fnr=12345678910")

    }
}
