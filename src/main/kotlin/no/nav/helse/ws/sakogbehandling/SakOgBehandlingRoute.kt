package no.nav.helse.ws.sakogbehandling

import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.respond
import no.nav.helse.ws.AktørId

fun Route.sakOgBehandling(sakOgBehandlingService: SakOgBehandlingService) {

    get("api/sakogbehandling/{aktorId}") {
        sakOgBehandlingService.finnSakOgBehandling(AktørId(call.parameters["aktorId"]!!))
                .respond(call)
    }
}
