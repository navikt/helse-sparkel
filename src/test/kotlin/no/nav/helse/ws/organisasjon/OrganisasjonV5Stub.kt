package no.nav.helse.ws.organisasjon

import no.nav.tjeneste.virksomhet.organisasjon.v5.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.*

class OrganisasjonV5Stub: OrganisasjonV5 {

    override fun hentOrganisasjon(request: HentOrganisasjonRequest?): HentOrganisasjonResponse {
        return HentOrganisasjonResponse().apply {
            organisasjon = Organisasjon().apply {
                navn = UstrukturertNavn().apply {
                    navnelinje.addAll(listOf("fornavn", "mellomnavn", "etternavn"))
                }
            }
        }
    }

    override fun ping() {
        TODO("not implemented")
    }

    override fun finnOrganisasjon(request: FinnOrganisasjonRequest?): FinnOrganisasjonResponse {
        TODO("not implemented")
    }

    override fun hentOrganisasjonsnavnBolk(request: HentOrganisasjonsnavnBolkRequest?): HentOrganisasjonsnavnBolkResponse {
        TODO("not implemented")
    }

    override fun hentNoekkelinfoOrganisasjon(request: HentNoekkelinfoOrganisasjonRequest?): HentNoekkelinfoOrganisasjonResponse {
        TODO("not implemented")
    }

    override fun validerOrganisasjon(request: ValiderOrganisasjonRequest?): ValiderOrganisasjonResponse {
        TODO("not implemented")
    }

    override fun hentVirksomhetsOrgnrForJuridiskOrgnrBolk(request: HentVirksomhetsOrgnrForJuridiskOrgnrBolkRequest?): HentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse {
        TODO("not implemented")
    }

    override fun finnOrganisasjonsendringerListe(request: FinnOrganisasjonsendringerListeRequest?): FinnOrganisasjonsendringerListeResponse {
        TODO("not implemented")
    }

}
