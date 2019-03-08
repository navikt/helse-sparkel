package no.nav.helse.ws.organisasjon

import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrganisasjonsMapperTest {

    @Test
    fun `skal mappe tomt navn`() {
        assertEquals(Organisasjon("1234", no.nav.helse.ws.organisasjon.Organisasjon.Type.Organisasjon, null), OrganisasjonsMapper.fraOrganisasjon(Organisasjon().apply {
            orgnummer = "1234"
            navn = UstrukturertNavn()
        }))
    }

    @Test
    fun `skal mappe en linje`() {
        assertEquals(Organisasjon("1234", no.nav.helse.ws.organisasjon.Organisasjon.Type.Organisasjon, "NAV"), OrganisasjonsMapper.fraOrganisasjon(Organisasjon().apply {
            orgnummer = "1234"
            navn = UstrukturertNavn().apply {
                with (navnelinje) {
                    add("NAV")
                }
            }
        }))
    }

    @Test
    fun `skal mappe flere linjer`() {
        assertEquals(Organisasjon("1234", no.nav.helse.ws.organisasjon.Organisasjon.Type.Organisasjon, "NAV, AVD SANNERGATA 2"), OrganisasjonsMapper.fraOrganisasjon(Organisasjon().apply {
            orgnummer = "1234"
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
        assertEquals(Organisasjon("1234", no.nav.helse.ws.organisasjon.Organisasjon.Type.Organisasjon, "NAV, AVD SANNERGATA 2, OSLO"), OrganisasjonsMapper.fraOrganisasjon(Organisasjon().apply {
            orgnummer = "1234"
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
