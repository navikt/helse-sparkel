package no.nav.helse.ws.organisasjon

import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.respond

private const val ATTRIBUTT_QUERY_PARAM = "attributt"
private const val ORG_NR_PATH_PARAM = "orgnr"

fun Route.organisasjon(organisasjonService: OrganisasjonService) {

    get("api/organisasjon/{orgnr}") {
        call.parameters["orgnr"]?.let {
            OrganisasjonsNummer(it)
        }?.let {
            organisasjonService.hentOrganisasjon(it)
                    .respond(call)
        }
    }
}
