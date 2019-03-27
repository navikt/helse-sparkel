package no.nav.helse.ws.organisasjon

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Either
import no.nav.helse.common.toLocalDate
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.OrgnrForOrganisasjon
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UnntakForOrgnr
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonResponse
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OrganisasjonClientTest {

    @Test
    fun `Henting av organisasjon med en navnelinje`() {
        val orgNr = "971524960"

        val organisasjonV5 = mockk<OrganisasjonV5>()
        every {
            organisasjonV5.hentOrganisasjon(match {
                it.orgnummer == orgNr
            })
        } returns HentOrganisasjonResponse().apply {
            organisasjon = Organisasjon().apply {
                orgnummer = orgNr
                navn = UstrukturertNavn().apply {
                    with (navnelinje) {
                        add("STORTINGET")
                    }
                }
            }
        }

        val client = OrganisasjonClient(organisasjonV5)
        val actual = client.hentOrganisasjon(Organisasjonsnummer(orgNr))

        when (actual) {
            is Either.Right -> assertEquals("STORTINGET", (actual.right.navn as UstrukturertNavn).navnelinje[0])
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal svare med feil`() {
        val orgNr = "971524960"

        val organisasjonV5 = mockk<OrganisasjonV5>()
        every {
            organisasjonV5.hentOrganisasjon(any())
        } throws(Exception("SOAP fault"))

        val client = OrganisasjonClient(organisasjonV5)
        val actual = client.hentOrganisasjon(Organisasjonsnummer(orgNr))

        when (actual) {
            is Either.Left -> assertEquals("SOAP fault", actual.left.message)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal hente virksomheter for organisasjon`() {
        val juridiskOrgnr = "889640782"
        val virksomhetOrgnr = "995298775"

        val organisasjonV5 = mockk<OrganisasjonV5>()
        every {
            organisasjonV5.hentVirksomhetsOrgnrForJuridiskOrgnrBolk(match {
                it.organisasjonsfilterListe.size == 1
                        && it.organisasjonsfilterListe[0].organisasjonsnummer == juridiskOrgnr
                        && it.organisasjonsfilterListe[0].hentingsdato.toLocalDate() == LocalDate.now()
            })
        } returns HentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse().apply {
            with (orgnrForOrganisasjonListe) {
                add(OrgnrForOrganisasjon().apply {
                    organisasjonsnummer = virksomhetOrgnr
                    juridiskOrganisasjonsnummer = juridiskOrgnr
                })
            }
        }

        val client = OrganisasjonClient(organisasjonV5)
        val actual = client.hentVirksomhetForJuridiskOrganisasjonsnummer(Organisasjonsnummer(juridiskOrgnr))

        when (actual) {
            is Either.Right -> {
                assertEquals(0, actual.right.unntakForOrgnrListe.size)
                assertEquals(1, actual.right.orgnrForOrganisasjonListe.size)
                assertEquals(virksomhetOrgnr, actual.right.orgnrForOrganisasjonListe[0].organisasjonsnummer)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal svare med unntaksliste`() {
        val juridiskOrgnr = "889640782"

        val organisasjonV5 = mockk<OrganisasjonV5>()
        every {
            organisasjonV5.hentVirksomhetsOrgnrForJuridiskOrgnrBolk(match {
                it.organisasjonsfilterListe.size == 1
                        && it.organisasjonsfilterListe[0].organisasjonsnummer == juridiskOrgnr
                        && it.organisasjonsfilterListe[0].hentingsdato.toLocalDate() == LocalDate.now()
            })
        } returns HentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse().apply {
            with (unntakForOrgnrListe) {
                add(UnntakForOrgnr().apply {
                    organisasjonsnummer = juridiskOrgnr
                    unntaksmelding = "$juridiskOrgnr er opphørt eller eksisterer ikke på dato ${LocalDate.now()}"
                })
            }
        }

        val client = OrganisasjonClient(organisasjonV5)
        val actual = client.hentVirksomhetForJuridiskOrganisasjonsnummer(Organisasjonsnummer(juridiskOrgnr))

        when (actual) {
            is Either.Right -> {
                assertEquals(1, actual.right.unntakForOrgnrListe.size)
                assertEquals(0, actual.right.orgnrForOrganisasjonListe.size)
                assertEquals(juridiskOrgnr, actual.right.unntakForOrgnrListe[0].organisasjonsnummer)
                assertEquals("$juridiskOrgnr er opphørt eller eksisterer ikke på dato ${LocalDate.now()}", actual.right.unntakForOrgnrListe[0].unntaksmelding)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal gi feil når oppslag av virksomheter for organisasjon feiler`() {
        val orgNr = "971524960"

        val organisasjonV5 = mockk<OrganisasjonV5>()
        every {
            organisasjonV5.hentVirksomhetsOrgnrForJuridiskOrgnrBolk(any())
        } throws(Exception("SOAP fault"))

        val client = OrganisasjonClient(organisasjonV5)
        val actual = client.hentVirksomhetForJuridiskOrganisasjonsnummer(Organisasjonsnummer(orgNr), LocalDate.now())

        when (actual) {
            is Either.Left -> assertEquals("SOAP fault", actual.left.message)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }
}
