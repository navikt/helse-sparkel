package no.nav.helse.ws.sykepenger

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.HttpFeil
import no.nav.helse.respond
import no.nav.helse.respondFeil
import no.nav.helse.ws.AktørId
import java.time.LocalDate

fun Route.sykepengeListe(sykepengerService: SykepengelisteService) {

    get("api/sykepengeperiode/{aktorId}") {
        sykepengerService.finnSykepengeperioder(AktørId(call.parameters["aktorId"]!!)).respond(call)
    }
}
