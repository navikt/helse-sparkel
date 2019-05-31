package no.nav.helse.domene.aiy.organisasjon

import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.aiy.organisasjon.domain.DriverVirksomhet
import no.nav.helse.domene.aiy.organisasjon.domain.InngårIJuridiskEnhet
import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon
import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.InngaarIJuridiskEnhet
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.JuridiskEnhet
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Virksomhet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OrganisasjonsMapperTest {

    @Test
    fun `tomGyldighetsperiode kan være null for InngårIJuridiskEnhet`() {
        assertEquals(InngårIJuridiskEnhet(Organisasjonsnummer("889640782"), LocalDate.parse("2019-01-01"), null), OrganisasjonsMapper.tilInngårIJuridiskEnhet(InngaarIJuridiskEnhet().apply {
            juridiskEnhet = JuridiskEnhet().apply {
                orgnummer ="889640782"
            }
            fomGyldighetsperiode = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
            tomGyldighetsperiode = null
        }))
    }

    @Test
    fun `tomGyldighetsperiode kan være null for DriverVirksomhet`() {
        assertEquals(DriverVirksomhet(Organisasjon.Virksomhet(Organisasjonsnummer("889640782"), "NAV"), LocalDate.parse("2019-01-01"), null), OrganisasjonsMapper.tilDriverVirksomhet(no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.DriverVirksomhet().apply {
            virksomhet = Virksomhet().apply {
                orgnummer = "889640782"
                navn = UstrukturertNavn().apply {
                    with (navnelinje) {
                        add("NAV")
                    }
                }
            }
            fomGyldighetsperiode = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
            tomGyldighetsperiode = null
        }))
    }

    @Test
    fun `skal mappe tomt navn`() {
        assertEquals(no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer("889640782"), null), OrganisasjonsMapper.fraOrganisasjon(Virksomhet().apply {
            orgnummer = "889640782"
            navn = UstrukturertNavn()
        }))
    }

    @Test
    fun `skal mappe en linje`() {
        assertEquals(no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer("889640782"), "NAV"), OrganisasjonsMapper.fraOrganisasjon(Virksomhet().apply {
            orgnummer = "889640782"
            navn = UstrukturertNavn().apply {
                with (navnelinje) {
                    add("NAV")
                }
            }
        }))
    }

    @Test
    fun `skal mappe flere linjer`() {
        assertEquals(no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer("889640782"), "NAV, AVD SANNERGATA 2"), OrganisasjonsMapper.fraOrganisasjon(Virksomhet().apply {
            orgnummer = "889640782"
            navn = UstrukturertNavn().apply {
                with (navnelinje) {
                    add("NAV")
                    add("AVD SANNERGATA 2")
                }
            }
        }))
    }

    @Test
    fun `skal mappe linjer med hull i`() {
        assertEquals(no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer("889640782"), "NAV, AVD SANNERGATA 2, OSLO"), OrganisasjonsMapper.fraOrganisasjon(Virksomhet().apply {
            orgnummer = "889640782"
            navn = UstrukturertNavn().apply {
                with (navnelinje) {
                    add("NAV")
                    add(null)
                    add("AVD SANNERGATA 2")
                    add(null)
                    add("OSLO")
                }
            }
        }))
    }
}
