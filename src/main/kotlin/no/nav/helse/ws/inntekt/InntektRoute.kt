package no.nav.helse.ws.inntekt

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post

fun Route.inntekt(factory: () -> InntektClient) {
    val inntektClient by lazy(factory)

    post("api/inntekt/inntekt-liste") {
        val fødselsnummer = call.receive<String>()

        val inntektListeResponse = inntektClient.hentInntektListe(fødselsnummer)

        call.respond(inntektListeResponse)
    }
}
