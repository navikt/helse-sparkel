package no.nav.helse.ws.inntekt

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.OppslagResult
import no.nav.helse.http.aktør.AktørregisterClient
import no.nav.helse.ws.Fødselsnummer
import java.time.YearMonth

fun Route.inntekt(factory: () -> InntektClient,
                  aktørregisterClientFactory: () -> AktørregisterClient
) {
    val inntektClient by lazy(factory)
    val aktørregisterClient by lazy(aktørregisterClientFactory)

    get("api/inntekt/{aktorId}") {
        if (!call.request.queryParameters.contains("fom") || !call.request.queryParameters.contains("tom")) {
            call.respond(HttpStatusCode.BadRequest, "you need to supply query parameter fom and tom")
        } else {
            val fom = YearMonth.parse(call.request.queryParameters["fom"]!!)
            val tom = YearMonth.parse(call.request.queryParameters["tom"]!!)

            val fnr = Fødselsnummer(aktørregisterClient.gjeldendeNorskIdent(call.parameters["aktorId"]!!))

            val lookupResult = inntektClient.hentInntektListe(fnr, fom, tom)
            when (lookupResult) {
                is OppslagResult.Ok -> call.respond(lookupResult.data)
                is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
            }
        }
    }
}
