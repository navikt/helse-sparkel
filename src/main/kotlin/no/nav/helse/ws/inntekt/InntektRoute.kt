package no.nav.helse.ws.inntekt

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*

fun Route.inntekt(inntektClient: InntektClient) {
    post("api/inntekt/inntekt-liste") {
        val fødselsnummer = call.receive<String>()

        val inntektListeResponse = inntektClient.hentInntektListe(fødselsnummer)

        call.respond(inntektListeResponse)
    }
}
