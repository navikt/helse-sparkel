package no.nav.helse.ws.sykepenger

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import org.joda.time.DateTime

fun Route.sykepengeListe(factory: () -> SykepengerClient) {
    val sykepenger by lazy(factory)

    get("api/sykepengeListe/{id}") {


        call.parameters["id"]?.let { id ->
            val fom = call.request.queryParameters["fom"]
            val tom = call.request.queryParameters["tom"]
            val lookupResult: OppslagResult = sykepenger.finnSykepengeVedtak(id, parseDate(fom), parseDate(tom))
            when (lookupResult) {
                is Success<*> -> call.respond(lookupResult.data!!)
                is Failure -> call.respond(HttpStatusCode.InternalServerError, "that didn't go so well...")
            }
        } ?: call.respond(HttpStatusCode.BadRequest, "you need to supply aktorId")


    }

}

fun parseDate(date: String?): DateTime {
    return date?.let {
        DateTime.parse(it)
    } ?: DateTime.now()
}
