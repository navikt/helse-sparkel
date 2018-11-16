package no.nav.helse.ws.person

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import no.nav.helse.ws.Fødselsnummer

fun Routing.person(personClient: PersonClient) {
    post("api/person") {
        val fødselsnummer = call.receive<String>()

        val person = personClient.personInfo(Fødselsnummer(fødselsnummer))

        call.respond(person)
    }
}
