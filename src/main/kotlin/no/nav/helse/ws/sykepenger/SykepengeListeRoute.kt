package no.nav.helse.ws.sykepenger

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.HttpFeil
import no.nav.helse.Either
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

            val lookupResult = sykepengerService.finnSykepengevedtak(AktørId(call.parameters["aktorId"]!!), fom, tom)
            when (lookupResult) {
                is Either.Right -> call.respond(lookupResult.right)
                is Either.Left -> call.respondFeil(lookupResult.left)
            }
        }
    }
}
