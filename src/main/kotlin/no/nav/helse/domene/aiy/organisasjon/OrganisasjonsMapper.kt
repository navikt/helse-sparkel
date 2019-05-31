package no.nav.helse.domene.aiy.organisasjon

import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.aiy.organisasjon.domain.DriverVirksomhet
import no.nav.helse.domene.aiy.organisasjon.domain.InngårIJuridiskEnhet
import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon
import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.*
import org.slf4j.LoggerFactory

object OrganisasjonsMapper {
    private val log = LoggerFactory.getLogger("OrganisasjonsMapper")

    fun fraOrganisasjon(organisasjon: no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon) : Organisasjon? {
        return Organisasjonsnummer(organisasjon.orgnummer).let { orgnr ->
            name(organisasjon.navn).let { navn ->
                when (organisasjon) {
                    is Orgledd -> Organisasjon.Organisasjonsledd(orgnr, navn, organisasjon.driverVirksomhet.map(::tilDriverVirksomhet), organisasjon.inngaarIJuridiskEnhet.map(::tilInngårIJuridiskEnhet))
                    is JuridiskEnhet -> Organisasjon.JuridiskEnhet(orgnr, navn, organisasjon.driverVirksomhet.map(::tilDriverVirksomhet))
                    is Virksomhet -> Organisasjon.Virksomhet(orgnr, navn, organisasjon.inngaarIJuridiskEnhet.map(::tilInngårIJuridiskEnhet))
                    else -> {
                        log.error("unknown organisasjonstype: ${organisasjon.javaClass.name}")
                        null
                    }
                }
            }
        }
    }

    fun tilDriverVirksomhet(driverVirksomhet: no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.DriverVirksomhet) =
            DriverVirksomhet(tilVirksomhet(driverVirksomhet.virksomhet),
                    driverVirksomhet.fomGyldighetsperiode.toLocalDate(),
                    driverVirksomhet.tomGyldighetsperiode?.toLocalDate())

    fun tilVirksomhet(virksomhet: Virksomhet) =
            Organisasjon.Virksomhet(Organisasjonsnummer(virksomhet.orgnummer), name(virksomhet.navn))

    fun tilInngårIJuridiskEnhet(inngårIJuridiskEnhet: InngaarIJuridiskEnhet) =
            InngårIJuridiskEnhet(Organisasjonsnummer(inngårIJuridiskEnhet.juridiskEnhet.orgnummer),
                    inngårIJuridiskEnhet.fomGyldighetsperiode.toLocalDate(),
                    inngårIJuridiskEnhet.tomGyldighetsperiode?.toLocalDate())

    private fun name(sammensattNavn: SammensattNavn): String? {
        val medNavn=  (sammensattNavn as UstrukturertNavn).navnelinje.filterNot {  it.isNullOrBlank() }
        return if (medNavn.isNullOrEmpty()) null
        else medNavn.joinToString(", ")
    }
}
