package no.nav.helse.ws.organisasjon.client

import arrow.core.Try
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjonsfilter
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonRequest
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentVirksomhetsOrgnrForJuridiskOrgnrBolkRequest
import java.time.LocalDate

class OrganisasjonClient(private val organisasjonV5: OrganisasjonV5) {

    fun hentOrganisasjon(orgnr: Organisasjonsnummer) =
            Try {
                organisasjonV5.hentOrganisasjon(hentOrganisasjonRequst(orgnr)).organisasjon
            }

    fun hentVirksomhetForJuridiskOrganisasjonsnummer(orgnr: Organisasjonsnummer, dato: LocalDate = LocalDate.now()) =
            Try {
                organisasjonV5.hentVirksomhetsOrgnrForJuridiskOrgnrBolk(hentVirksomhetsOrgnrForJuridiskOrgnrBolkRequest(orgnr, dato))
            }

    private fun hentOrganisasjonRequst(orgnr: Organisasjonsnummer) =
            HentOrganisasjonRequest().apply {
                orgnummer = orgnr.value
            }

    private fun hentVirksomhetsOrgnrForJuridiskOrgnrBolkRequest(orgnr: Organisasjonsnummer, dato: LocalDate = LocalDate.now()) =
            HentVirksomhetsOrgnrForJuridiskOrgnrBolkRequest().apply {
                with(organisasjonsfilterListe) {
                    add(Organisasjonsfilter().apply {
                        organisasjonsnummer = orgnr.value
                        hentingsdato = dato.toXmlGregorianCalendar()
                    })
                }
            }
}




