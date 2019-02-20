package no.nav.helse.ws.inntekt

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.OppslagResult
import no.nav.helse.ws.AktørId
import java.time.YearMonth

fun Route.inntekt(inntektService: InntektService) {

    get("api/inntekt/{aktorId}") {
        if (!call.request.queryParameters.contains("fom") || !call.request.queryParameters.contains("tom")) {
            call.respond(HttpStatusCode.BadRequest, "you need to supply query parameter fom and tom")
        } else {
            val fom = YearMonth.parse(call.request.queryParameters["fom"]!!)
            val tom = YearMonth.parse(call.request.queryParameters["tom"]!!)

            val lookupResult = inntektService.hentInntekter(AktørId(call.parameters["aktorId"]!!), fom, tom)
            when (lookupResult) {
                is OppslagResult.Ok -> call.respond(lookupResult.data)
                is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
            }
        }
    }
}
