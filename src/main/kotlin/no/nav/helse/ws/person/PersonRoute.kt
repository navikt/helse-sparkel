package no.nav.helse.ws.person

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.receiveJson
import no.nav.helse.ws.*

private const val aktørParam = "aktor"

fun Route.person(factory: () -> PersonClient) {
    val personClient: PersonClient by lazy(factory)

    post("api/person") {
        call.receiveJson().let { json ->
            if (!json.has(aktørParam)) {
                call.respond(HttpStatusCode.BadRequest, "you need to supply $aktørParam=1234567891011")
            } else {
                val lookupResult: OppslagResult = personClient.personInfo(AktørId(json.getString(aktørParam)))
                when (lookupResult) {
                    is Success<*> -> call.respond(lookupResult.data!!)
                    is Failure -> call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "that didn't go so well..."))
                }
            }
        }

    }
}

