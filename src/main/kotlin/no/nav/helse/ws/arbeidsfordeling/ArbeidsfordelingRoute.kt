package no.nav.helse.ws.arbeidsfordeling

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.Failure
import no.nav.helse.Success
import no.nav.helse.ws.AktørId

private const val HOVED_AKTOER_PATH_PARAM = "hovedAktoerId"
private const val MED_AKTOER_QUERY_PARAM = "medAktoerId"
private const val TEMA_QUERY_PARAM = "tema"

fun Route.arbeidsfordeling(factory: () -> ArbeidsfordelingService) {
    val arbeidsfordelingService: ArbeidsfordelingService by lazy(factory)

    get("api/arbeidsfordeling/behandlende-enhet/{$HOVED_AKTOER_PATH_PARAM}") {
        val tema = call.getTema()
        if (tema == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Requesten må inneholde query parameter '$TEMA_QUERY_PARAM'"))
        } else {
            val hovedAktoerId = call.getHovedAktoerId()
            val medAktoerIder = call.getMedAktoerIder()

            val oppslagResult = arbeidsfordelingService.getBehandlendeEnhet(
                    hovedAktoer = hovedAktoerId,
                    tema = tema,
                    medAktoerer = medAktoerIder
            )

            when (oppslagResult) {
                is Success<*> -> call.respond(oppslagResult.data!!)
                is Failure -> call.respond(HttpStatusCode.InternalServerError, oppslagResult.errors)
            }
        }
    }
}

private fun ApplicationCall.getMedAktoerIder(): List<AktørId> {
    val stringList = request.queryParameters.getAll(MED_AKTOER_QUERY_PARAM)
    if (stringList.isNullOrEmpty()) return listOf()
    val medAktoerer = mutableListOf<AktørId>()
    stringList.forEach { it ->
        medAktoerer.add(AktørId(it))
    }
    return medAktoerer.toList()
}

private fun ApplicationCall.getHovedAktoerId(): AktørId {
    return AktørId(parameters[HOVED_AKTOER_PATH_PARAM]!!)
}

private fun ApplicationCall.getTema() : Tema? {
    return if (request.queryParameters.contains(TEMA_QUERY_PARAM)) Tema(request.queryParameters[TEMA_QUERY_PARAM]!!) else null
}
