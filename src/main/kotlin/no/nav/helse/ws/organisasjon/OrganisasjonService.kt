package no.nav.helse.ws.organisasjon

import no.nav.helse.map

class OrganisasjonService(private val organisasjonsClient: OrganisasjonClient) {

    fun hentOrganisasjonnavn(orgnr: OrganisasjonsNummer) = hentOrganisasjon(orgnr, listOf(OrganisasjonsAttributt("navn"))).map {
        it.navn ?: ""
    }
    fun hentOrganisasjon(orgnr: OrganisasjonsNummer, attributter : List<OrganisasjonsAttributt>) = organisasjonsClient.hentOrganisasjon(orgnr, attributter)
}
