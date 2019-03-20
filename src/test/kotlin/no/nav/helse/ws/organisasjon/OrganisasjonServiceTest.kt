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
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.OrgnrForOrganisasjon
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UnntakForOrgnr
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse
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

    @Test
    fun `skal mappe feil til UkjentFeil`() {
        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentVirksomhetForJuridiskOrganisasjonsnummer(any())
        } returns Either.Left(Exception())

        val actual = OrganisasjonService(organisasjon).hentVirksomhetForJuridiskOrganisasjonsnummer(
                OrganisasjonsNummer("1234"))

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.UkjentFeil, actual.left)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal mappe unntaksliste til IkkeFunnet`() {
        val juridiskOrgNr = "1234"

        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentVirksomhetForJuridiskOrganisasjonsnummer(any())
        } returns Either.Right(HentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse().apply {
            with(unntakForOrgnrListe) {
                add(UnntakForOrgnr().apply {
                    organisasjonsnummer = juridiskOrgNr
                })
            }
        })

        val actual = OrganisasjonService(organisasjon).hentVirksomhetForJuridiskOrganisasjonsnummer(
                OrganisasjonsNummer(juridiskOrgNr))

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.IkkeFunnet, actual.left)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal hente orgnr for virksomhet i en juridisk enhet`() {
        val juridiskOrgNr = "1234"
        val virksomhetOrgNr = "4321"

        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentVirksomhetForJuridiskOrganisasjonsnummer(any())
        } returns Either.Right(HentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse().apply {
            with(orgnrForOrganisasjonListe) {
                add(OrgnrForOrganisasjon().apply {
                    juridiskOrganisasjonsnummer = juridiskOrgNr
                    organisasjonsnummer = virksomhetOrgNr
                })
            }
        })

        val actual = OrganisasjonService(organisasjon).hentVirksomhetForJuridiskOrganisasjonsnummer(
                OrganisasjonsNummer(juridiskOrgNr))

        when (actual) {
            is Either.Right -> assertEquals(virksomhetOrgNr, actual.right.value)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal gi IkkeFunnet dersom virksomhetsoppslag gir resultat på feil juridisk enhet`() {
        val juridiskOrgNr = "1234"
        val enAnnenJuridiskOrgNr = "554433"
        val virksomhetOrgNr = "4321"

        val organisasjon = mockk<OrganisasjonClient>()
        every {
            organisasjon.hentVirksomhetForJuridiskOrganisasjonsnummer(any())
        } returns Either.Right(HentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse().apply {
            with(orgnrForOrganisasjonListe) {
                add(OrgnrForOrganisasjon().apply {
                    juridiskOrganisasjonsnummer = enAnnenJuridiskOrgNr
                    organisasjonsnummer = virksomhetOrgNr
                })
            }
        })

        val actual = OrganisasjonService(organisasjon).hentVirksomhetForJuridiskOrganisasjonsnummer(
                OrganisasjonsNummer(juridiskOrgNr))

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.IkkeFunnet, actual.left)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }
}
