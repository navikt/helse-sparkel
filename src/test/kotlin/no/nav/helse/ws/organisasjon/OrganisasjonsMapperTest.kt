package no.nav.helse.ws.organisasjon

import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Virksomhet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrganisasjonsMapperTest {

    @Test
    fun `skal mappe tomt navn`() {
        assertEquals(no.nav.helse.ws.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer("889640782"), null), OrganisasjonsMapper.fraOrganisasjon(Virksomhet().apply {
            orgnummer = "889640782"
            navn = UstrukturertNavn()
        }))
    }

    @Test
    fun `skal mappe en linje`() {
        assertEquals(no.nav.helse.ws.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer("889640782"), "NAV"), OrganisasjonsMapper.fraOrganisasjon(Virksomhet().apply {
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
        assertEquals(no.nav.helse.ws.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer("889640782"), "NAV, AVD SANNERGATA 2"), OrganisasjonsMapper.fraOrganisasjon(Virksomhet().apply {
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
        assertEquals(no.nav.helse.ws.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer("889640782"), "NAV, AVD SANNERGATA 2, OSLO"), OrganisasjonsMapper.fraOrganisasjon(Virksomhet().apply {
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
