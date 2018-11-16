package no.nav.helse

sealed class OppslagResult

data class Success<T>(val data: T): OppslagResult()

data class Failure(val errors: List<String>): OppslagResult()

