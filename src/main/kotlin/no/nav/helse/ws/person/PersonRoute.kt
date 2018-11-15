package no.nav.helse.ws.person

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post

fun Routing.person(personClient: PersonClient) {
    post("api/person/geografisk-tilknytning") {
        val fødselsnummer = call.receive<String>()

        val geografiskTilknytningResponse = personClient.getGeografiskTilknytning(fødselsnummer)

        call.respond(geografiskTilknytningResponse)
    }
}
