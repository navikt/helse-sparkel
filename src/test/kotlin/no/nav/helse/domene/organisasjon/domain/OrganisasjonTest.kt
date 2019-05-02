package no.nav.helse.domene.organisasjon.domain

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
        assertEquals("JuridiskEnhet(orgnr=Organisasjonsnummer(value=889640782), navn=null, virksomheter=[])", Organisasjon.JuridiskEnhet(orgnr).toString())
        assertEquals("Organisasjonsledd(orgnr=Organisasjonsnummer(value=889640782), navn=null, virksomheter=[], inngårIJuridiskEnhet=[])", Organisasjon.Organisasjonsledd(orgnr).toString())
        assertEquals("Virksomhet(orgnr=Organisasjonsnummer(value=889640782), navn=null, inngårIJuridiskEnhet=[])", Organisasjon.Virksomhet(orgnr).toString())
    }
}
