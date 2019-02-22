package no.nav.helse.ws.arbeidsforhold

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.OppslagResult
import no.nav.helse.ws.AktørId
import java.time.LocalDate

fun Route.arbeidsforhold(
        arbeidsforholdService: ArbeidsforholdService
) {
    get("api/arbeidsforhold/{aktorId}") {
        if (!call.request.queryParameters.contains("fom") || !call.request.queryParameters.contains("tom")) {
            call.respond(HttpStatusCode.BadRequest, "you need to supply query parameter fom and tom")
        } else {
            val fom = LocalDate.parse(call.request.queryParameters["fom"]!!)
            val tom = LocalDate.parse(call.request.queryParameters["tom"]!!)

            val lookupResult = arbeidsforholdService.finnArbeidsforhold(AktørId(call.parameters["aktorId"]!!), fom, tom)

            when (lookupResult) {
                is OppslagResult.Ok -> call.respond(ArbeidsforholdResponse(lookupResult.data))
                is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
            }
        }
    }

    get("api/arbeidsgivere/{aktorId}") {
        if (!call.request.queryParameters.contains("fom") || !call.request.queryParameters.contains("tom")) {
            call.respond(HttpStatusCode.BadRequest, "you need to supply query parameter fom and tom")
        } else {
            val fom = LocalDate.parse(call.request.queryParameters["fom"]!!)
            val tom = LocalDate.parse(call.request.queryParameters["tom"]!!)

            val lookupResult = arbeidsforholdService.finnArbeidsgivere(AktørId(call.parameters["aktorId"]!!), fom, tom)

            when (lookupResult) {
                is OppslagResult.Ok -> call.respond(ArbeidsgivereResponse(lookupResult.data))
                is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
            }
        }
    }
}

data class ArbeidsforholdResponse(val arbeidsforhold: List<Arbeidsforhold>)
data class ArbeidsgivereResponse(val arbeidsgivere: List<Arbeidsgiver>)
