package no.nav.helse.ws.arbeidsforhold

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.HttpFeil
import no.nav.helse.map
import no.nav.helse.respond
import no.nav.helse.respondFeil
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.arbeidsforhold.dto.ArbeidsforholdResponse
import no.nav.helse.ws.arbeidsforhold.dto.ArbeidsgivereResponse
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

            arbeidsforholdService.finnArbeidsforhold(AktørId(call.parameters["aktorId"]!!), fom, tom).map {
                it.map(ArbeidDtoMapper::toDto)
            }.map {
                ArbeidsforholdResponse(it)
            }.respond(call)
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

            arbeidsforholdService.finnArbeidsgivere(AktørId(call.parameters["aktorId"]!!), fom, tom).map {
                it.map(ArbeidDtoMapper::toDto)
            }.map {
                ArbeidsgivereResponse(it)
            }.respond(call)
        }
    }
}
