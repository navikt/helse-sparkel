package no.nav.helse.ws.sakogbehandling

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.*
import no.nav.helse.ws.*

fun Route.sakOgBehandling(sakOgBehandlingService: SakOgBehandlingService) {

    get("api/sakogbehandling/{aktorId}") {
        val lookupResult = sakOgBehandlingService.finnSakOgBehandling(AktørId(call.parameters["aktorId"]!!))
        when (lookupResult) {
            is OppslagResult.Ok -> call.respond(lookupResult.data)
            is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
        }
    }
}
