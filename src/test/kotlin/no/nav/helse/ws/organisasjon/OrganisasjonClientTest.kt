package no.nav.helse.ws.organisasjon

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Either
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentNoekkelinfoOrganisasjonResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class OrganisasjonClientTest {

    @Test
    fun `Henting av organisasjon med en navnelinje`() {
        val orgNr = "971524960"

        val organisasjonV5 = mockk<OrganisasjonV5>()
        every {
            organisasjonV5.hentNoekkelinfoOrganisasjon(match {
                it.orgnummer == orgNr && it.gyldigDato == null
            })
        } returns HentNoekkelinfoOrganisasjonResponse().apply {
            navn = UstrukturertNavn().apply {
                with (navnelinje) {
                    add("STORTINGET")
                }
            }
        }

        val client = OrganisasjonClient(organisasjonV5)
        val actual = client.hentOrganisasjon(OrganisasjonsNummer(orgNr), listOf(OrganisasjonsAttributt("navn")))

        when (actual) {
            is Either.Right -> assertEquals("STORTINGET", actual.right.navn)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `Henting av organisasjon med tre navnelinjer`() {
        val orgNr = "971524960"

        val organisasjonV5 = mockk<OrganisasjonV5>()
        every {
            organisasjonV5.hentNoekkelinfoOrganisasjon(match {
                it.orgnummer == orgNr && it.gyldigDato == null
            })
        } returns HentNoekkelinfoOrganisasjonResponse().apply {
            navn = UstrukturertNavn().apply {
                with (navnelinje) {
                    add("NAV")
                    add("AVD SANNERGATA 2")
                    add("ARBEIDS- OG VELFERDSDIREKTORATET")
                }
            }
        }

        val client = OrganisasjonClient(organisasjonV5)
        val actual = client.hentOrganisasjon(OrganisasjonsNummer(orgNr), listOf(OrganisasjonsAttributt("navn")))

        when (actual) {
            is Either.Right -> assertEquals("NAV, AVD SANNERGATA 2, ARBEIDS- OG VELFERDSDIREKTORATET", actual.right.navn)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `Henting av organisasjon uten navnelinjer`() {
        val orgNr = "971524960"

        val organisasjonV5 = mockk<OrganisasjonV5>()
        every {
            organisasjonV5.hentNoekkelinfoOrganisasjon(match {
                it.orgnummer == orgNr && it.gyldigDato == null
            })
        } returns HentNoekkelinfoOrganisasjonResponse().apply {
            navn = UstrukturertNavn()
        }

        val client = OrganisasjonClient(organisasjonV5)
        val actual = client.hentOrganisasjon(OrganisasjonsNummer(orgNr), listOf(OrganisasjonsAttributt("navn")))

        when (actual) {
            is Either.Right -> assertNull(actual.right.navn)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `Henting av ikke supportert attributt`() {
        val orgNr = "971524960"

        val organisasjonV5 = mockk<OrganisasjonV5>()

        val client = OrganisasjonClient(organisasjonV5)
        val actual = client.hentOrganisasjon(OrganisasjonsNummer(orgNr), listOf(OrganisasjonsAttributt("adresse")))

        when (actual) {
            is Either.Left -> assertTrue(actual.left is UkjentAttributtException)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `Henting uten attributt`() {
        val orgNr = "971524960"

        val organisasjonV5 = mockk<OrganisasjonV5>()
        every {
            organisasjonV5.hentNoekkelinfoOrganisasjon(match {
                it.orgnummer == orgNr && it.gyldigDato == null
            })
        } returns HentNoekkelinfoOrganisasjonResponse().apply {
            navn = UstrukturertNavn().apply {
                with (navnelinje) {
                    add("STORTINGET")
                }
            }
        }

        val client = OrganisasjonClient(organisasjonV5)
        val actual = client.hentOrganisasjon(OrganisasjonsNummer(orgNr))

        when (actual) {
            is Either.Right -> assertEquals("STORTINGET", actual.right.navn)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal svare med feil`() {
        val orgNr = "971524960"

        val organisasjonV5 = mockk<OrganisasjonV5>()
        every {
            organisasjonV5.hentNoekkelinfoOrganisasjon(any())
        } throws(Exception("SOAP fault"))

        val client = OrganisasjonClient(organisasjonV5)
        val actual = client.hentOrganisasjon(OrganisasjonsNummer(orgNr), emptyList())

        when (actual) {
            is Either.Left -> assertEquals("SOAP fault", actual.left.message)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }
}
