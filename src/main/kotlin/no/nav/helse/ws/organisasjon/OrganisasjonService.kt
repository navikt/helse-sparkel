package no.nav.helse.ws.organisasjon

import no.nav.helse.Feilårsak
import no.nav.helse.bimap
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentNoekkelinfoOrganisasjonOrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentNoekkelinfoOrganisasjonUgyldigInput

class OrganisasjonService(private val organisasjonsClient: OrganisasjonClient) {

    fun hentOrganisasjon(orgnr: OrganisasjonsNummer, attributter : List<OrganisasjonsAttributt>) =
            organisasjonsClient.hentOrganisasjon(orgnr, attributter).bimap({
                when (it) {
                    is HentNoekkelinfoOrganisasjonOrganisasjonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentNoekkelinfoOrganisasjonUgyldigInput -> Feilårsak.FeilFraTjeneste
                    is UkjentAttributtException -> Feilårsak.IkkeImplementert
                    else -> Feilårsak.UkjentFeil
                }
            }, {
                OrganisasjonsMapper.fraNoekkelInfo(it)
            })
}

data class OrganisasjonResponse(val navn: String?)
