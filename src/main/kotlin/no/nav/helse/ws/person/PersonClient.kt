package no.nav.helse.ws.person

import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest


class PersonClient(private val person: PersonV3) {
    fun getGeografiskTilknytning(fødelsnummer: String): GeografiskTilknytningResponse {
        val request = HentGeografiskTilknytningRequest()

        request.aktoer = PersonIdent().apply {
            ident = NorskIdent().apply { ident = fødelsnummer }
        }

        val response = person.hentGeografiskTilknytning(request)

        return GeografiskTilknytningResponse(
                response.geografiskTilknytning.geografiskTilknytning,
                response.diskresjonskode?.kodeverksRef)
    }

    data class GeografiskTilknytningResponse(
            val geografiskTilknytning: String,
            val diskresjonskode: String?
    )
}
