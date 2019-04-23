package no.nav.helse.domene.aktør

import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.respond
import no.nav.helse.domene.AktørId

fun Route.fnrForAktør(aktørregisterService: AktørregisterService) {
    get("api/aktor/{aktorId}/fnr") {
        aktørregisterService.fødselsnummerForAktør(AktørId(call.parameters["aktorId"]!!)).respond(call)
    }
}
