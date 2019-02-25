package no.nav.helse.ws.inntekt

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.HttpFeil
import no.nav.helse.OppslagResult
import no.nav.helse.respondFeil
import no.nav.helse.ws.AktørId
import java.time.YearMonth
import java.time.format.DateTimeParseException

fun Route.inntekt(inntektService: InntektService) {

    get("api/inntekt/{aktorId}") {
        if (!call.request.queryParameters.contains("fom") || !call.request.queryParameters.contains("tom")) {
            call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "you need to supply query parameter fom and tom"))
        } else {
            val fom = try {
                YearMonth.parse(call.request.queryParameters["fom"]!!)
            } catch (err: DateTimeParseException) {
                call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "fom must be specified as yyyy-mm"))
                return@get
            }
            val tom = try {
                YearMonth.parse(call.request.queryParameters["tom"]!!)
            } catch (err: DateTimeParseException) {
                call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "tom must be specified as yyyy-mm"))
                return@get
            }

            val lookupResult = inntektService.hentInntekter(AktørId(call.parameters["aktorId"]!!), fom, tom)
            when (lookupResult) {
                is OppslagResult.Ok -> call.respond(lookupResult.data)
                is OppslagResult.Feil -> call.respondFeil(lookupResult.feil)
            }
        }
    }
}
