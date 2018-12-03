package no.nav.helse.ws.organisasjon

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success

fun Route.organisasjon(factory: () -> OrganisasjonClient) {
    val orgClient by lazy(factory)

    get("api/organisasjon") {
        call.parameters["orgnr"]?.let { orgnr ->
            val lookupResult: OppslagResult = orgClient.orgNavn(orgnr)
            when (lookupResult) {
                is Success<*> -> call.respond(lookupResult.data!!)
                is Failure -> call.respond(HttpStatusCode.InternalServerError, "that didn't go so well...")
            }
        } ?: call.respond(HttpStatusCode.BadRequest, "you need to supply orgnr=12345678")

    }
}
