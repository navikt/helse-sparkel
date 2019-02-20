package no.nav.helse.ws.sakogbehandling

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.helse.OppslagResult
import no.nav.helse.receiveJson
import no.nav.helse.ws.AktørId

fun Route.sakOgBehandling(sakOgBehandlingService: SakOgBehandlingService) {

    post("api/sakogbehandling") {
        call.receiveJson().let { json ->
            if (!json.has("aktorId")) {
                call.respond(HttpStatusCode.BadRequest, "you need to supply aktorId=12345678910")
            } else {
                val lookupResult = sakOgBehandlingService.finnSakOgBehandling(AktørId(json.getString("aktorId")))
                when (lookupResult) {
                    is OppslagResult.Ok -> call.respond(lookupResult.data)
                    is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
                }
            }
        }
    }
}
