package no.nav.helse

import io.ktor.http.HttpStatusCode

sealed class Feil {
    data class Feilmelding(val feilmelding: String): Feil()
    data class Exception(val feilmelding: String, val exception: Throwable): Feil()
}

sealed class OppslagResult<out E, out S> {

    data class Feil<out E>(val httpCode: HttpStatusCode, val feil: E) : OppslagResult<E, Nothing>()

    data class Ok<out S>(val data: S) : OppslagResult<Nothing, S>()
}

fun <A, B, C> OppslagResult<A, B>.map(mapper: (B) -> C) =
    flatMap {
        OppslagResult.Ok(mapper(it))
    }

fun <A, B, C> OppslagResult<A, B>.flatMap(mapper: (B) -> OppslagResult<A, C>) =
    when (this) {
        is OppslagResult.Ok -> mapper(this.data)
        is OppslagResult.Feil -> this
    }

fun <A, B, C> OppslagResult<A, B>.fold(ifLeft: (A) -> C, ifRight: (B) -> C) =
    when (this) {
        is OppslagResult.Ok -> ifRight(this.data)
        is OppslagResult.Feil -> ifLeft(this.feil)
    }

fun <B> OppslagResult<*, B>.orElse(provider: () -> B) = fold({ provider() }, { it })