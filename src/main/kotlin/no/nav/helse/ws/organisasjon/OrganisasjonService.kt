package no.nav.helse.ws.organisasjon

import no.nav.helse.Feilårsak
import no.nav.helse.bimap
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentOrganisasjonOrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentOrganisasjonUgyldigInput

class OrganisasjonService(private val organisasjonsClient: OrganisasjonClient) {

    fun hentOrganisasjon(orgnr: OrganisasjonsNummer) =
            organisasjonsClient.hentOrganisasjon(orgnr).bimap({
                when (it) {
                    is HentOrganisasjonOrganisasjonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentOrganisasjonUgyldigInput -> Feilårsak.FeilFraBruker
                    else -> Feilårsak.UkjentFeil
                }
            }, {
                OrganisasjonsMapper.fraOrganisasjon(it)
            })
}

data class Organisasjon(val orgnr: String, val type: Type, val navn: String?) {
    enum class Type {
        Orgledd,
        JuridiskEnhet,
        Virksomhet,
        Organisasjon
    }
}
