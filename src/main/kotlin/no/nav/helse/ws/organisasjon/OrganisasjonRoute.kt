package no.nav.helse.ws.organisasjon

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.OppslagResult

private const val ATTRIBUTT_QUERY_PARAM = "attributt"
private const val ORG_NR_PATH_PARAM = "orgnr"

fun Route.organisasjon(orgClient: OrganisasjonClient) {

    get("api/organisasjon/{$ORG_NR_PATH_PARAM}") {
        val organisasjonsNummer = call.getOrganisasjonsNummer()
        val attributter = call.getAttributes()

        val lookupResult = orgClient.hentOrganisasjon(
                orgnr = organisasjonsNummer,
                attributter = attributter
        )

        when (lookupResult) {
            is OppslagResult.Ok -> call.respond(lookupResult.data)
            is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
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
