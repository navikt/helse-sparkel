package no.nav.helse.ws.organisasjon

import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentNoekkelinfoOrganisasjonResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrganisasjonsMapperTest {

    @Test
    fun `skal mappe tomt navn`() {
        assertEquals(OrganisasjonResponse(null), OrganisasjonsMapper.fraNoekkelInfo(HentNoekkelinfoOrganisasjonResponse().apply {
            navn = UstrukturertNavn()
        }))
    }

    @Test
    fun `skal mappe en linje`() {
        assertEquals(OrganisasjonResponse("NAV"), OrganisasjonsMapper.fraNoekkelInfo(HentNoekkelinfoOrganisasjonResponse().apply {
            navn = UstrukturertNavn().apply {
                with (navnelinje) {
                    add("NAV")
                }
            }
        }))
    }

    @Test
    fun `skal mappe flere linjer`() {
        assertEquals(OrganisasjonResponse("NAV, AVD SANNERGATA 2"), OrganisasjonsMapper.fraNoekkelInfo(HentNoekkelinfoOrganisasjonResponse().apply {
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
        assertEquals(OrganisasjonResponse("NAV, AVD SANNERGATA 2, OSLO"), OrganisasjonsMapper.fraNoekkelInfo(HentNoekkelinfoOrganisasjonResponse().apply {
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
