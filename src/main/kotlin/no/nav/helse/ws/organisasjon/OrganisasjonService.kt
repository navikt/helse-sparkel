package no.nav.helse.ws.organisasjon

class OrganisasjonService(private val organisasjonsClient: OrganisasjonClient) {

    fun hentOrganisasjon(orgnr: OrganisasjonsNummer, attributter : List<OrganisasjonsAttributt>) = organisasjonsClient.hentOrganisasjon(orgnr, attributter)
}
