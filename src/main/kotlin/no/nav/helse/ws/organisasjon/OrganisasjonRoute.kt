package no.nav.helse.ws.organisasjon

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.Failure
import no.nav.helse.Success

private const val ATTRIBUTT_QUERY_PARAM = "attributt"
private const val ORG_NR_PATH_PARAM = "orgnr"

fun Route.organisasjon(factory: () -> OrganisasjonClient) {
    val orgClient by lazy(factory)

    get("api/organisasjon/{$ORG_NR_PATH_PARAM}") {
        val organisasjonsNummer = call.getOrganisasjonsNummer()
        val attributter = call.getAttributes()

        val oppslagResult = orgClient.hentOrganisasjon(
                orgnr = organisasjonsNummer,
                attributter = attributter
        )

        when (oppslagResult) {
            is Success<*> -> call.respond(oppslagResult.data!!)
            is Failure -> call.respond(oppslagResult.httpCode, mapOf("errors" to oppslagResult.errors))
        }
    }
}

private fun ApplicationCall.getOrganisasjonsNummer(): OrganisasjonsNummer {
    return OrganisasjonsNummer(parameters[ORG_NR_PATH_PARAM]!!)
}

private fun ApplicationCall.getAttributes() : List<OrganisasjonsAttributt>{
    val stringList = request.queryParameters.getAll(ATTRIBUTT_QUERY_PARAM)
    if (stringList.isNullOrEmpty()) return listOf()
    val attributter = mutableListOf<OrganisasjonsAttributt>()
    stringList.forEach { it ->
        attributter.add(OrganisasjonsAttributt(it))
    }
    return attributter.toList()
}
