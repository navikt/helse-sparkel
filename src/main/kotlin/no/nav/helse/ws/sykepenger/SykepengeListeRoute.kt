package no.nav.helse.ws.sykepenger

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.OppslagResult
import no.nav.helse.http.aktør.AktørregisterClient
import no.nav.helse.ws.Fødselsnummer
import java.time.LocalDate

fun Route.sykepengeListe(sykepenger: SykepengerClient,
                         aktørregisterClient: AktørregisterClient) {

    get("api/sykepengevedtak/{aktorId}") {
        if (!call.request.queryParameters.contains("fom") || !call.request.queryParameters.contains("tom")) {
            call.respond(HttpStatusCode.BadRequest, "you need to supply query parameter fom and tom")
        } else {
            val fom = LocalDate.parse(call.request.queryParameters["fom"]!!)
            val tom = LocalDate.parse(call.request.queryParameters["tom"]!!)

            val fnr = Fødselsnummer(aktørregisterClient.gjeldendeNorskIdent(call.parameters["aktorId"]!!))

            val lookupResult = sykepenger.finnSykepengeVedtak(fnr, fom, tom)
            when (lookupResult) {
                is OppslagResult.Ok -> call.respond(lookupResult.data)
                is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
            }
        }
    }
}
