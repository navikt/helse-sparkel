package no.nav.helse.domene.aiy.organisasjon

import arrow.core.Either
import arrow.core.Try
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Feilårsak
import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjonsnummer
import no.nav.helse.oppslag.organisasjon.OrganisasjonClient
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentOrganisasjonOrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentOrganisasjonUgyldigInput
import no.nav.tjeneste.virksomhet.organisasjon.v5.feil.OrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v5.feil.UgyldigInput
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.JuridiskEnhet
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class OrganisasjonServiceTest {

    @Test
    fun `skal svare med organisasjon`() {
        val orgNr = "889640782"
        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentOrganisasjon(match {
                it.value == orgNr
            })
        } returns Try.Success(JuridiskEnhet().apply {
            orgnummer = orgNr
            navn = UstrukturertNavn().apply {
                with (navnelinje) {
                    add("NAV")
                }
            }
        })

        val actual = OrganisasjonService(organisasjon).hentOrganisasjon(Organisasjonsnummer(orgNr))

        when (actual) {
            is Either.Right -> assertEquals("NAV", actual.b.navn)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal mappe HentNoekkelinfoOrganisasjonOrganisasjonIkkeFunnet til IkkeFunnet`() {
        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentOrganisasjon(any())
        } returns Try.Failure(HentOrganisasjonOrganisasjonIkkeFunnet("SOAP fault", OrganisasjonIkkeFunnet()))

        val actual = OrganisasjonService(organisasjon).hentOrganisasjon(Organisasjonsnummer("889640782"))

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.IkkeFunnet, actual.a)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal mappe HentOrganisasjonUgyldigInput til FeilFraBruker`() {
        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentOrganisasjon(any())
        } returns Try.Failure(HentOrganisasjonUgyldigInput("SOAP fault", UgyldigInput()))

        val actual = OrganisasjonService(organisasjon).hentOrganisasjon(Organisasjonsnummer("889640782"))

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.FeilFraBruker, actual.a)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal mappe Exception til UkjentFeil`() {
        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentOrganisasjon(any())
        } returns Try.Failure(Exception())

        val actual = OrganisasjonService(organisasjon).hentOrganisasjon(Organisasjonsnummer("889640782"))

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.UkjentFeil, actual.a)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }
}
