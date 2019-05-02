package no.nav.helse.oppslag.organisasjon

import arrow.core.Try
import no.nav.helse.domene.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonRequest

class OrganisasjonClient(private val organisasjonV5: OrganisasjonV5) {

    fun hentOrganisasjon(orgnr: Organisasjonsnummer) =
            Try {
                organisasjonV5.hentOrganisasjon(hentOrganisasjonRequst(orgnr)).organisasjon
            }

    private fun hentOrganisasjonRequst(orgnr: Organisasjonsnummer) =
            HentOrganisasjonRequest().apply {
                orgnummer = orgnr.value
                isInkluderHierarki = true
                isInkluderHistorikk = true
            }
}




