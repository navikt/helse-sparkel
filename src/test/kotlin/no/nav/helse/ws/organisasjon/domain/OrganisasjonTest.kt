package no.nav.helse.ws.organisasjon.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrganisasjonTest {

    @Test
    fun `skal angi type`() {
        val orgnr = Organisasjonsnummer("889640782")
        assertEquals("JuridiskEnhet", Organisasjon.JuridiskEnhet(orgnr).type())
        assertEquals("Orgledd", Organisasjon.Organisasjonsledd(orgnr).type())
        assertEquals("Virksomhet", Organisasjon.Virksomhet(orgnr).type())
    }

    @Test
    fun `toString skal printe ut organisasjonstype og orgnummer`() {
        val orgnr = Organisasjonsnummer("889640782")
        assertEquals("JuridiskEnhet(orgnr=Organisasjonsnummer(value=889640782), navn=null)", Organisasjon.JuridiskEnhet(orgnr).toString())
        assertEquals("Organisasjonsledd(orgnr=Organisasjonsnummer(value=889640782), navn=null)", Organisasjon.Organisasjonsledd(orgnr).toString())
        assertEquals("Virksomhet(orgnr=Organisasjonsnummer(value=889640782), navn=null)", Organisasjon.Virksomhet(orgnr).toString())
    }
}
