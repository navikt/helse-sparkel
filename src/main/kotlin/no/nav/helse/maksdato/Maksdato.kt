package no.nav.helse.maksdato

import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.core.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.*
import org.json.*
import java.time.*
import java.time.format.*

fun Route.maksdato(serviceUrl: String) {
    post("/maksdato") {
        val convertedRequest = call.receiveJson().toMaksdatoRequest()
        when (convertedRequest) {
            is Success -> {
                val response = makeMaksdatoRequest(serviceUrl, convertedRequest.body)
                if (response.statusCode == 200) {
                    call.respond(HttpStatusCode.OK, String(response.data))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "")
                }
            }
            is Failure -> call.respond("Error while looking up maksdato: ${convertedRequest.errMsg}")
        }
    }

}

internal fun makeMaksdatoRequest(url: String, body: MaksdatoRequest): Response {
    val (_, response, _) = url.httpPost()
            .jsonBody(body.toString())
            .header(mapOf(
                    "Nav-Consumer-Id" to "sparkel"
            )).responseString()
    return response
}

internal fun JSONObject.toMaksdatoRequest(): ConverterResult {
    return try {
        val førsteFraværsdag = getString("førsteFraværsdag").toDate()
        val førsteSykepengedag = getString("førsteSykepengedag").toDate()
        val personensAlder = getInt("personensAlder")
        val tidligerePerioder = getJSONArray("tidligerePerioder")
                .asSequence()
                .map { it as JSONObject }
                .map { it.toTidsperiode() }
                .toList()
        Success(MaksdatoRequest(
                førsteFraværsdag,
                førsteSykepengedag,
                personensAlder, "ARBEIDSTAKER",
                tidligerePerioder))
    } catch (ex: Exception) {
        Failure("'$this' is not a valid request")
    }
}

data class MaksdatoRequest(
        val førsteFraværsdag: LocalDate,
        val førsteSykepengedag: LocalDate,
        val personensAlder: Int,
        val yrkesstatus: String = "ARBEIDSTAKER",
        val tidligerePerioder: List<Tidsperiode>)

data class Tidsperiode(val fom: LocalDate, val tom: LocalDate)

private fun String.toDate(): LocalDate = LocalDate.parse(this, DateTimeFormatter.ISO_DATE)

private fun JSONObject.toTidsperiode() =
        Tidsperiode(getString("fom").toDate(), getString("tom").toDate())

sealed class ConverterResult
data class Success(val body: MaksdatoRequest): ConverterResult()
data class Failure(val errMsg: String): ConverterResult()