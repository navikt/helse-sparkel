package no.nav.helse.ws.organisasjon

import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.SammensattNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentNoekkelinfoOrganisasjonResponse

object OrganisasjonsMapper {
    fun fraNoekkelInfo(
            response : HentNoekkelinfoOrganisasjonResponse) : OrganisasjonResponse {
        return OrganisasjonResponse(navn = name(response.navn))
    }

    private fun name(sammensattNavn: SammensattNavn): String? {
        val medNavn=  (sammensattNavn as UstrukturertNavn).navnelinje.filterNot {  it.isNullOrBlank() }
        return if (medNavn.isNullOrEmpty()) null
        else medNavn.joinToString(", ")
    }
}