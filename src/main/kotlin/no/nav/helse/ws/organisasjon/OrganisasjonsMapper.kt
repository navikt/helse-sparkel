package no.nav.helse.ws.organisasjon

import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.JuridiskEnhet
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Orgledd
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.SammensattNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Virksomhet

object OrganisasjonsMapper {
    fun fraOrganisasjon(organisasjon: no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon) : Organisasjon {
        return Organisasjon(
                orgnr = Organisasjonsnummer(organisasjon.orgnummer),
                navn = name(organisasjon.navn),
                type = when (organisasjon) {
                    is Orgledd -> Organisasjon.Type.Orgledd
                    is JuridiskEnhet -> Organisasjon.Type.JuridiskEnhet
                    is Virksomhet -> Organisasjon.Type.Virksomhet
                    else -> Organisasjon.Type.Organisasjon
                }
        )
    }

    private fun name(sammensattNavn: SammensattNavn): String? {
        val medNavn=  (sammensattNavn as UstrukturertNavn).navnelinje.filterNot {  it.isNullOrBlank() }
        return if (medNavn.isNullOrEmpty()) null
        else medNavn.joinToString(", ")
    }
}
