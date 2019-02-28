package no.nav.helse.ws.organisasjon

import no.nav.helse.Feilårsak
import no.nav.helse.Either
import no.nav.helse.map
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentNoekkelinfoOrganisasjonOrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentNoekkelinfoOrganisasjonUgyldigInput

class OrganisasjonService(private val organisasjonsClient: OrganisasjonClient) {

    fun hentOrganisasjonnavn(orgnr: OrganisasjonsNummer) = hentOrganisasjon(orgnr, listOf(OrganisasjonsAttributt("navn"))).map {
        it.navn ?: ""
    }
    fun hentOrganisasjon(orgnr: OrganisasjonsNummer, attributter : List<OrganisasjonsAttributt>): Either<Feilårsak, OrganisasjonResponse> {
        val lookupResult = organisasjonsClient.hentOrganisasjon(orgnr, attributter)
        return when (lookupResult) {
            is Either.Right -> lookupResult
            is Either.Left -> {
                Either.Left(when (lookupResult.left) {
                    is HentNoekkelinfoOrganisasjonOrganisasjonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentNoekkelinfoOrganisasjonUgyldigInput -> Feilårsak.FeilFraTjeneste
                    is UkjentAttributtException -> Feilårsak.IkkeImplementert
                    else -> Feilårsak.FeilFraTjeneste
                })
            }
        }
    }
}
