package no.nav.helse.oppslag.organisasjon

import arrow.core.Try
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonRequest

class OrganisasjonClient(private val organisasjonV5: OrganisasjonV5) {

    fun hentOrganisasjon(orgnr: String) =
            Try {
                organisasjonV5.hentOrganisasjon(hentOrganisasjonRequst(orgnr)).organisasjon
            }

    private fun hentOrganisasjonRequst(orgnr: String) =
            HentOrganisasjonRequest().apply {
                orgnummer = orgnr
                isInkluderHierarki = true
                isInkluderHistorikk = true
            }
}




