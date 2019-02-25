package no.nav.helse.ws.arbeidsforhold

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.HttpFeil
import no.nav.helse.OppslagResult
import no.nav.helse.respondFeil
import no.nav.helse.ws.AktørId
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun Route.arbeidsforhold(
        arbeidsforholdService: ArbeidsforholdService
) {
    get("api/arbeidsforhold/{aktorId}") {
        if (!call.request.queryParameters.contains("fom") || !call.request.queryParameters.contains("tom")) {
            call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "you need to supply query parameter fom and tom"))
        } else {
            val fom = try {
                LocalDate.parse(call.request.queryParameters["fom"]!!)
            } catch (err: DateTimeParseException) {
                call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "fom must be specified as yyyy-mm-dd"))
                return@get
            }
            val tom = try {
                LocalDate.parse(call.request.queryParameters["tom"]!!)
            } catch (err: DateTimeParseException) {
                call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "tom must be specified as yyyy-mm-dd"))
                return@get
            }

            val lookupResult = arbeidsforholdService.finnArbeidsforhold(AktørId(call.parameters["aktorId"]!!), fom, tom)

            when (lookupResult) {
                is OppslagResult.Ok -> call.respond(ArbeidsforholdResponse(lookupResult.data))
                is OppslagResult.Feil -> call.respondFeil(lookupResult.feil)
            }
        }
    }

    get("api/arbeidsgivere/{aktorId}") {
        if (!call.request.queryParameters.contains("fom") || !call.request.queryParameters.contains("tom")) {
            call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "you need to supply query parameter fom and tom"))
        } else {
            val fom = try {
                LocalDate.parse(call.request.queryParameters["fom"]!!)
            } catch (err: DateTimeParseException) {
                call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "fom must be specified as yyyy-mm-dd"))
                return@get
            }
            val tom = try {
                LocalDate.parse(call.request.queryParameters["tom"]!!)
            } catch (err: DateTimeParseException) {
                call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "tom must be specified as yyyy-mm-dd"))
                return@get
            }

            val lookupResult = arbeidsforholdService.finnArbeidsgivere(AktørId(call.parameters["aktorId"]!!), fom, tom)

            when (lookupResult) {
                is OppslagResult.Ok -> call.respond(ArbeidsgivereResponse(lookupResult.data))
                is OppslagResult.Feil -> call.respondFeil(lookupResult.feil)
            }
        }
    }
}

data class ArbeidsforholdResponse(val arbeidsforhold: List<Arbeidsforhold>)
data class ArbeidsgivereResponse(val arbeidsgivere: List<Arbeidsgiver>)
