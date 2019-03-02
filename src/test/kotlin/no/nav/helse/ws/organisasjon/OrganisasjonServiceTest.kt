package no.nav.helse.ws.organisasjon

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Either
import no.nav.helse.Feilårsak
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentNoekkelinfoOrganisasjonOrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentNoekkelinfoOrganisasjonUgyldigInput
import no.nav.tjeneste.virksomhet.organisasjon.v5.feil.OrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v5.feil.UgyldigInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class OrganisasjonServiceTest {

    @Test
    fun `skal mappe HentNoekkelinfoOrganisasjonOrganisasjonIkkeFunnet til IkkeFunnet`() {
        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentOrganisasjon(any(), any())
        } returns Either.Left(HentNoekkelinfoOrganisasjonOrganisasjonIkkeFunnet("SOAP fault", OrganisasjonIkkeFunnet()))

        val actual = OrganisasjonService(organisasjon).hentOrganisasjon(
                OrganisasjonsNummer("1234"),
                listOf(OrganisasjonsAttributt("navn"))
        )

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.IkkeFunnet, actual.left)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal mappe HentNoekkelinfoOrganisasjonUgyldigInput til FeilFraTjeneste`() {
        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentOrganisasjon(any(), any())
        } returns Either.Left(HentNoekkelinfoOrganisasjonUgyldigInput("SOAP fault", UgyldigInput()))

        val actual = OrganisasjonService(organisasjon).hentOrganisasjon(
                OrganisasjonsNummer("1234"),
                listOf(OrganisasjonsAttributt("navn"))
        )

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.FeilFraTjeneste, actual.left)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal mappe UkjentAttributtException til IkkeImplementert`() {
        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentOrganisasjon(any(), any())
        } returns Either.Left(UkjentAttributtException())

        val actual = OrganisasjonService(organisasjon).hentOrganisasjon(
                OrganisasjonsNummer("1234"),
                listOf(OrganisasjonsAttributt("navn"))
        )

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.IkkeImplementert, actual.left)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal mappe Exception til UkjentFeil`() {
        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentOrganisasjon(any(), any())
        } returns Either.Left(Exception())

        val actual = OrganisasjonService(organisasjon).hentOrganisasjon(
                OrganisasjonsNummer("1234"),
                listOf(OrganisasjonsAttributt("navn"))
        )

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.UkjentFeil, actual.left)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }
}
