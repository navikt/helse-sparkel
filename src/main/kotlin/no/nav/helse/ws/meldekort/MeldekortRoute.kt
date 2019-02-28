package no.nav.helse.ws.meldekort

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
import java.time.format.DateTimeFormatter

fun Route.meldekort(meldekortService: MeldekortService) {

    get("api/meldekort/") {
        if (listOf("aktorId", "fom", "tom").all(call.request.queryParameters::contains)) {
            val aktorId = call.request.queryParameters["aktorId"]!!
            val fom = call.request.queryParameters["fom"]!!.asLocalDate()
            val tom = call.request.queryParameters["tom"]!!.asLocalDate()

            val lookupResult = meldekortService.hentMeldekortgrunnlag(AktørId(aktorId), fom, tom)
            when (lookupResult) {
                is Either.Right -> call.respond(lookupResult.right)
                is Either.Left -> call.respondFeil(lookupResult.left)
            }
        } else {
            call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "Missing at least one parameter of `aktorId`, `fom` and `tom`"))
        }
    }
}

private fun String.asLocalDate(): LocalDate {
    return LocalDate.parse(this, DateTimeFormatter.ISO_DATE)
}
