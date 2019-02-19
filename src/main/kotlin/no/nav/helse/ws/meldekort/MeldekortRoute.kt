package no.nav.helse.ws.meldekort

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.OppslagResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun Route.meldekort(factory: () -> MeldekortClient) {
    val client: MeldekortClient by lazy(factory)

    get("api/meldekort/") {
        if (listOf("aktorId", "fom", "tom").all(call.request.queryParameters::contains)) {
            val aktorId = call.request.queryParameters["aktorId"]!!
            val fom = call.request.queryParameters["fom"]!!.asLocalDate()
            val tom = call.request.queryParameters["tom"]!!.asLocalDate()
            val lookupResult = client.hentMeldekortgrunnlag(aktorId, fom, tom)
            when (lookupResult) {
                is OppslagResult.Ok -> call.respond(lookupResult.data)
                is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
            }
        } else {
            call.respond(HttpStatusCode.BadRequest, "Missing at least one parameter of `aktorId`, `fom` and `tom`")
        }
    }

    get("api/meldekort/ping") {
        val lookupResult = client.ping()
        when (lookupResult) {
            is OppslagResult.Ok -> call.respond(lookupResult.data)
            is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
        }
    }
}

private fun String.asLocalDate(): LocalDate {
    return LocalDate.parse(this, DateTimeFormatter.ISO_DATE)
}
