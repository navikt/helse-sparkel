package no.nav.helse.oppslag.organisasjon

import arrow.core.Try
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.domene.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class OrganisasjonClientTest {

    @Test
    fun `Henting av organisasjon med en navnelinje`() {
        val orgNr = "971524960"

        val organisasjonV5 = mockk<OrganisasjonV5>()
        every {
            organisasjonV5.hentOrganisasjon(match {
                it.orgnummer == orgNr && it.isInkluderHierarki == true && it.isInkluderHistorikk == true
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
}
