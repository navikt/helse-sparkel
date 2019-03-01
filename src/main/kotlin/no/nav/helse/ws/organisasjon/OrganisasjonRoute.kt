package no.nav.helse.ws.organisasjon

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.respond

private const val ATTRIBUTT_QUERY_PARAM = "attributt"
private const val ORG_NR_PATH_PARAM = "orgnr"

fun Route.organisasjon(organisasjonService: OrganisasjonService) {

    get("api/organisasjon/{$ORG_NR_PATH_PARAM}") {
        val organisasjonsNummer = call.getOrganisasjonsNummer()
        val attributter = call.getAttributes()

        organisasjonService.hentOrganisasjon(organisasjonsNummer, attributter)
                .respond(call)
    }
}

private fun ApplicationCall.getOrganisasjonsNummer() = OrganisasjonsNummer(parameters[ORG_NR_PATH_PARAM]!!)

private fun ApplicationCall.getAttributes() =
        request.queryParameters.getAll(ATTRIBUTT_QUERY_PARAM)
                ?.map {
                    OrganisasjonsAttributt(it)
                } ?: emptyList()
