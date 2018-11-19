package no.nav.helse.ws.organisasjon

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.*

fun Routing.organisasjon(orgClient: OrganisasjonClient) {
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
