package no.nav.helse.ws.arbeidsfordeling

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.HttpFeil
import no.nav.helse.respond
import no.nav.helse.respondFeil
import no.nav.helse.ws.AktørId

private const val HOVED_AKTOER_PATH_PARAM = "hovedAktoerId"
private const val MED_AKTOER_QUERY_PARAM = "medAktoerId"
private const val TEMA_QUERY_PARAM = "tema"

fun Route.arbeidsfordeling(arbeidsfordelingService: ArbeidsfordelingService) {

    get("api/arbeidsfordeling/behandlende-enhet/{$HOVED_AKTOER_PATH_PARAM}") {
        val tema = call.getTema()
        if (tema == null) {
            call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "Requesten må inneholde query parameter '$TEMA_QUERY_PARAM'"))
        } else {
            val hovedAktoerId = call.getHovedAktoerId()
            val medAktoerIder = call.getMedAktoerIder()

            arbeidsfordelingService.getBehandlendeEnhet(
                    hovedAktoer = hovedAktoerId,
                    tema = tema,
                    medAktoerer = medAktoerIder
            ).respond(call)
        }
    }
}

private fun ApplicationCall.getMedAktoerIder() = request.queryParameters.getAll(MED_AKTOER_QUERY_PARAM)?.map {
        AktørId(it)
    } ?: emptyList()

private fun ApplicationCall.getHovedAktoerId() = AktørId(parameters[HOVED_AKTOER_PATH_PARAM]!!)

private fun ApplicationCall.getTema() = if (request.queryParameters.contains(TEMA_QUERY_PARAM)) Tema(request.queryParameters[TEMA_QUERY_PARAM]!!) else null
