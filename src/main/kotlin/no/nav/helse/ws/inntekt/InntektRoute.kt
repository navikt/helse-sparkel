package no.nav.helse.ws.inntekt

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.receiveJson

fun Route.inntekt(factory: () -> InntektClient) {
    val inntektClient by lazy(factory)

    post("api/inntekt/inntekt-liste") {
        call.receiveJson().let { json ->
            if (!json.has("fnr")) {
                call.respond(HttpStatusCode.BadRequest, "you need to supply fnr=12345678910")
            } else {
                val lookupResult: OppslagResult = inntektClient.hentInntektListe(json.getString("fnr"))
                when (lookupResult) {
                    is Success<*> -> call.respond(lookupResult.data!!)
                    is Failure -> call.respond(HttpStatusCode.InternalServerError, "that didn't go so well...")
                }
            }
        }
    }
}
