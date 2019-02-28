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

sealed class Either<out A, out R> {

    data class Left<out A>(val left: A) : Either<A, Nothing>()

    data class Right<out B>(val right: B) : Either<Nothing, B>()
}

fun <A, B, C> Either<A, B>.map(ifRight: (B) -> C) =
    flatMap {
        Either.Right(ifRight(it))
    }

fun <A, B, C> Either<A, B>.flatMap(ifRight: (B) -> Either<A, C>) =
    when (this) {
        is Either.Right -> ifRight(this.right)
        is Either.Left -> this
    }

fun <A, B, C> Either<A, B>.fold(ifLeft: (A) -> C, ifRight: (B) -> C) =
    when (this) {
        is Either.Right -> ifRight(this.right)
        is Either.Left -> ifLeft(this.left)
    }

fun <B> Either<*, B>.orElse(ifLeft: () -> B) = fold({ ifLeft() }, { it })
