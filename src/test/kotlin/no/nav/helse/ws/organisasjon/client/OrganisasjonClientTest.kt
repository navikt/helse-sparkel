package no.nav.helse.ws.organisasjon.client

import arrow.core.Try
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.common.toLocalDate
import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
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
            is Try.Success -> assertEquals("STORTINGET", (actual.value.navn as UstrukturertNavn).navnelinje[0])
            is Try.Failure -> fail { "Expected Try.Success to be returned" }
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
            is Try.Failure -> assertEquals("SOAP fault", actual.exception.message)
            is Try.Success -> fail { "Expected Try.Failure to be returned" }
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
            is Try.Success -> {
                assertEquals(0, actual.value.unntakForOrgnrListe.size)
                assertEquals(1, actual.value.orgnrForOrganisasjonListe.size)
                assertEquals(virksomhetOrgnr, actual.value.orgnrForOrganisasjonListe[0].organisasjonsnummer)
            }
            is Try.Failure -> fail { "Expected Try.Success to be returned" }
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
            is Try.Success -> {
                assertEquals(1, actual.value.unntakForOrgnrListe.size)
                assertEquals(0, actual.value.orgnrForOrganisasjonListe.size)
                assertEquals(juridiskOrgnr, actual.value.unntakForOrgnrListe[0].organisasjonsnummer)
                assertEquals("$juridiskOrgnr er opphørt eller eksisterer ikke på dato ${LocalDate.now()}", actual.value.unntakForOrgnrListe[0].unntaksmelding)
            }
            is Try.Failure -> fail { "Expected Try.Success to be returned" }
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
            is Try.Failure -> assertEquals("SOAP fault", actual.exception.message)
            is Try.Success -> fail { "Expected Try.Failure to be returned" }
        }
    }
}
