package no.nav.helse

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond

data class FeilResponse(val feilmelding: String)
data class HttpFeil(val status: HttpStatusCode, val feilmelding: String)

suspend fun ApplicationCall.respondFeil(feil: HttpFeil) = respond(feil.status, FeilResponse(feil.feilmelding))
suspend fun ApplicationCall.respondFeil(feil: Feilårsak) = respondFeil(feil.toHttpFeil())

sealed class Feilårsak {
    object IkkeFunnet: Feilårsak()
    object FeilFraTjeneste: Feilårsak()
    object UkjentFeil: Feilårsak()
    object IkkeImplementert: Feilårsak()
}

fun Feilårsak.toHttpFeil() = when (this) {
    is Feilårsak.IkkeFunnet -> HttpFeil(HttpStatusCode.NotFound, "Resource not found")
    is Feilårsak.FeilFraTjeneste -> HttpFeil(HttpStatusCode.InternalServerError, "Error while contacting external service")
    is Feilårsak.UkjentFeil -> HttpFeil(HttpStatusCode.InternalServerError, "Unknown error")
    is Feilårsak.IkkeImplementert -> HttpFeil(HttpStatusCode.NotImplemented, "Not implemented")
}

sealed class OppslagResult<out E, out S> {

    data class Feil<out E>(val feil: E) : OppslagResult<E, Nothing>()

    data class Ok<out S>(val data: S) : OppslagResult<Nothing, S>()
}

fun <A, B, C> OppslagResult<A, B>.map(ifRight: (B) -> C) =
    flatMap {
        OppslagResult.Ok(ifRight(it))
    }

fun <A, B, C> OppslagResult<A, B>.flatMap(ifRight: (B) -> OppslagResult<A, C>) =
    when (this) {
        is OppslagResult.Ok -> ifRight(this.data)
        is OppslagResult.Feil -> this
    }

fun <A, B, C> OppslagResult<A, B>.fold(ifLeft: (A) -> C, ifRight: (B) -> C) =
    when (this) {
        is OppslagResult.Ok -> ifRight(this.data)
        is OppslagResult.Feil -> ifLeft(this.feil)
    }

fun <B> OppslagResult<*, B>.orElse(ifLeft: () -> B) = fold({ ifLeft() }, { it })
