package no.nav.helse.ws.organisasjon

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Either
import no.nav.helse.Feilårsak
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentOrganisasjonOrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentOrganisasjonUgyldigInput
import no.nav.tjeneste.virksomhet.organisasjon.v5.feil.OrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v5.feil.UgyldigInput
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class OrganisasjonServiceTest {

    @Test
    fun `skal svare med organisasjon`() {
        val orgNr = "1234"
        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentOrganisasjon(match {
                it.value == orgNr
            })
        } returns Either.Right(Organisasjon().apply {
            orgnummer = orgNr
            navn = UstrukturertNavn().apply {
                with (navnelinje) {
                    add("NAV")
                }
            }
        })

        val actual = OrganisasjonService(organisasjon).hentOrganisasjon(OrganisasjonsNummer(orgNr))

        when (actual) {
            is Either.Right -> assertEquals("NAV", actual.right.navn)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal mappe HentNoekkelinfoOrganisasjonOrganisasjonIkkeFunnet til IkkeFunnet`() {
        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentOrganisasjon(any())
        } returns Either.Left(HentOrganisasjonOrganisasjonIkkeFunnet("SOAP fault", OrganisasjonIkkeFunnet()))

        val actual = OrganisasjonService(organisasjon).hentOrganisasjon(OrganisasjonsNummer("1234"))

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.IkkeFunnet, actual.left)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal mappe HentOrganisasjonUgyldigInput til FeilFraBruker`() {
        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentOrganisasjon(any())
        } returns Either.Left(HentOrganisasjonUgyldigInput("SOAP fault", UgyldigInput()))

        val actual = OrganisasjonService(organisasjon).hentOrganisasjon(OrganisasjonsNummer("1234"))

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.FeilFraBruker, actual.left)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal mappe Exception til UkjentFeil`() {
        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentOrganisasjon(any())
        } returns Either.Left(Exception())

        val actual = OrganisasjonService(organisasjon).hentOrganisasjon(OrganisasjonsNummer("1234"))

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.UkjentFeil, actual.left)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }
}
