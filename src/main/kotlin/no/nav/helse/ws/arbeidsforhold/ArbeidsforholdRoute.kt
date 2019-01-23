package no.nav.helse.ws.arbeidsforhold

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.*
import no.nav.helse.http.aktør.*
import no.nav.helse.ws.Fødselsnummer
import java.time.LocalDate

fun Route.arbeidsforhold(
        clientFactory: () -> ArbeidsforholdClient,
        aktørregisterClientFactory: () -> AktørregisterClient
) {
    val arbeidsforholdClient by lazy(clientFactory)
    val aktørregisterClient by lazy(aktørregisterClientFactory)

    get("api/arbeidsforhold/{aktorId}") {
        if (!call.request.queryParameters.contains("fom") || !call.request.queryParameters.contains("tom")) {
            call.respond(HttpStatusCode.BadRequest, "you need to supply query parameter fom and tom")
        } else {
            val fom = LocalDate.parse(call.request.queryParameters["fom"]!!)
            val tom = LocalDate.parse(call.request.queryParameters["tom"]!!)

            val fnr = Fødselsnummer(aktørregisterClient.gjeldendeNorskIdent(call.parameters["aktorId"]!!))

            val lookupResult: OppslagResult = arbeidsforholdClient.finnArbeidsforhold(fnr, fom, tom)
            when (lookupResult) {
                is Success<*> -> call.respond(lookupResult.data!!)
                is Failure -> call.respond(HttpStatusCode.InternalServerError, "that didn't go so well...")
            }
        }
    }
}

