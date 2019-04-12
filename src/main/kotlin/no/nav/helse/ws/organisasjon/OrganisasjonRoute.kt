package no.nav.helse.ws.organisasjon

import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.map
import no.nav.helse.respond
import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer

fun Route.organisasjon(organisasjonService: OrganisasjonService) {

    get("api/organisasjon/{orgnr}") {
        call.parameters["orgnr"]?.let {
            Organisasjonsnummer(it)
        }?.let {
            organisasjonService.hentOrganisasjon(it)
                    .map(OrganisasjonDtoMapper::toDto)
                    .respond(call)
        }
    }
}
