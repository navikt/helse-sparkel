package no.nav.helse.ws.organisasjon

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.receiveJson

fun Route.organisasjon(factory: () -> OrganisasjonClient) {
    val orgClient by lazy(factory)

    post("api/organisasjon") {
        call.receiveJson().let { json ->
            if (!json.has("orgnr")) {
                call.respond(HttpStatusCode.BadRequest, "you need to supply orgnr=12345678910")
            } else {
                val lookupResult: OppslagResult = orgClient.orgNavn(json.getString("orgnr"))
                when (lookupResult) {
                    is Success<*> -> call.respond(lookupResult.data!!)
                    is Failure -> call.respond(HttpStatusCode.InternalServerError, "that didn't go so well...")
                }
            }
        }
    }
}
