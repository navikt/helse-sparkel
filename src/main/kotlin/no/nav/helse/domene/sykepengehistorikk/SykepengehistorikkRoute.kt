package no.nav.helse.domene.sykepengehistorikk

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.HttpFeil
import no.nav.helse.domene.AktørId
import no.nav.helse.respond
import no.nav.helse.respondFeil
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun Route.sykepengehistorikk(sykepengehistorikkService: SykepengehistorikkService) {

    get("api/sykepengehistorikk/{aktorId}") {
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

            sykepengehistorikkService.hentSykepengeHistorikk(AktørId(call.parameters["aktorId"]!!), fom, tom).respond(call)
        }
    }
}
