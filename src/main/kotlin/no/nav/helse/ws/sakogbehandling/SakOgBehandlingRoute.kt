package no.nav.helse.ws.sakogbehandling

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.Either
import no.nav.helse.respondFeil
import no.nav.helse.ws.AktørId

fun Route.sakOgBehandling(sakOgBehandlingService: SakOgBehandlingService) {

    get("api/sakogbehandling/{aktorId}") {
        val lookupResult = sakOgBehandlingService.finnSakOgBehandling(AktørId(call.parameters["aktorId"]!!))
        when (lookupResult) {
            is Either.Right -> call.respond(lookupResult.right)
            is Either.Left -> call.respondFeil(lookupResult.left)
        }
    }
}
