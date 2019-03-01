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

    get("api/sykepengevedtak/{aktorId}") {
        if (!call.request.queryParameters.contains("fom") || !call.request.queryParameters.contains("tom")) {
            call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "you need to supply query parameter fom and tom"))
        } else {
            val fom = LocalDate.parse(call.request.queryParameters["fom"]!!)
            val tom = LocalDate.parse(call.request.queryParameters["tom"]!!)

            sykepengerService.finnSykepengevedtak(AktørId(call.parameters["aktorId"]!!), fom, tom)
                    .respond(call)
        }
    }
}
