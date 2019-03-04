package no.nav.helse

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond

data class FeilResponse(val feilmelding: String)
data class HttpFeil(val status: HttpStatusCode, val feilmelding: String)

suspend fun ApplicationCall.respondFeil(feil: HttpFeil) = respond(feil.status, FeilResponse(feil.feilmelding))

suspend fun <B: Any> Either<Feilårsak, B>.respond(call: ApplicationCall) = when (this) {
    is Either.Right -> call.respond(right)
    is Either.Left -> call.respondFeil(left.toHttpFeil())
}

sealed class Feilårsak {
    object FeilFraBruker: Feilårsak()
    object IkkeFunnet: Feilårsak()
    object FeilFraTjeneste: Feilårsak()
    object UkjentFeil: Feilårsak()
    object IkkeImplementert: Feilårsak()
}

fun Feilårsak.toHttpFeil() = when (this) {
    is Feilårsak.FeilFraBruker -> HttpFeil(HttpStatusCode.BadRequest, "Bad request")
    is Feilårsak.IkkeFunnet -> HttpFeil(HttpStatusCode.NotFound, "Resource not found")
    is Feilårsak.FeilFraTjeneste -> HttpFeil(HttpStatusCode.InternalServerError, "Error while contacting external service")
    is Feilårsak.UkjentFeil -> HttpFeil(HttpStatusCode.InternalServerError, "Unknown error")
    is Feilårsak.IkkeImplementert -> HttpFeil(HttpStatusCode.NotImplemented, "Not implemented")
}
