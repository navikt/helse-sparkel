package no.nav.helse.ws.organisasjon

import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.*
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.*

class OrganisasjonV5MisbehavingStub: OrganisasjonV5 {

    override fun hentOrganisasjon(request: HentOrganisasjonRequest?): HentOrganisasjonResponse {
        throw Exception("SOAPy stuff got besmirched")
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