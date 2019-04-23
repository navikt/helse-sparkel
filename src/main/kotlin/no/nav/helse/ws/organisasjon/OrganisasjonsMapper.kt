package no.nav.helse.ws.organisasjon

import no.nav.helse.common.toLocalDate
import no.nav.helse.ws.organisasjon.domain.InngårIJuridiskEnhet
import no.nav.helse.ws.organisasjon.domain.Organisasjon
import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.*
import org.slf4j.LoggerFactory

object OrganisasjonsMapper {
    private val log = LoggerFactory.getLogger("OrganisasjonsMapper")

    fun fraOrganisasjon(organisasjon: no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon) : Organisasjon? {
        return Organisasjonsnummer(organisasjon.orgnummer).let { orgnr ->
            name(organisasjon.navn).let { navn ->
                when (organisasjon) {
                    is Orgledd -> Organisasjon.Organisasjonsledd(orgnr, navn)
                    is JuridiskEnhet -> Organisasjon.JuridiskEnhet(orgnr, navn)
                    is Virksomhet -> Organisasjon.Virksomhet(orgnr, navn, organisasjon.inngaarIJuridiskEnhet.map { entry ->
                        InngårIJuridiskEnhet(Organisasjonsnummer(entry.juridiskEnhet.orgnummer), entry.fomGyldighetsperiode.toLocalDate(), entry.tomGyldighetsperiode?.toLocalDate())
                    })
                    else -> {
                        log.error("unknown organisasjonstype: ${organisasjon.javaClass.name}")
                        null
                    }
                }
            }
        }
    }

    private fun name(sammensattNavn: SammensattNavn): String? {
        val medNavn=  (sammensattNavn as UstrukturertNavn).navnelinje.filterNot {  it.isNullOrBlank() }
        return if (medNavn.isNullOrEmpty()) null
        else medNavn.joinToString(", ")
    }
}
