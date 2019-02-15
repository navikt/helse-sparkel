package no.nav.helse

import io.ktor.http.HttpStatusCode

sealed class OppslagResult

data class Success<T>(val data: T): OppslagResult()

data class Failure(val errors: List<String>, val httpCode : HttpStatusCode = HttpStatusCode.InternalServerError): OppslagResult()

