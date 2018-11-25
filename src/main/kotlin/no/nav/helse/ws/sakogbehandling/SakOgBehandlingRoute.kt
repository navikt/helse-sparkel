package no.nav.helse.ws.sakogbehandling

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success

fun Route.sakOgBehandling(sakOgBehandlingClient: SakOgBehandlingClient) {
    post("api/sakogbehandling") {
        call.receiveParameters()["aktorId"]?.let { aktorId ->
            val lookupResult: OppslagResult = sakOgBehandlingClient.finnSakOgBehandling(aktorId)
            when (lookupResult) {
                is Success<*> -> call.respond(lookupResult.data!!)
                is Failure -> call.respond(HttpStatusCode.InternalServerError, "that didn't go so well...")
            }
        } ?: call.respond(HttpStatusCode.BadRequest, "you need to supply aktorId")
    }
}
