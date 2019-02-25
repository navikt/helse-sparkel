package no.nav.helse.ws.person

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bydel
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Foedselsdato
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoenn
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoennstyper
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Postnummer
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate

class PersonClientTest {

    @Test
    fun `personInfo skal få feil når tjenesten kaster exception`() {
        val personV3 = mockk<PersonV3>()

        every {
            personV3.hentPerson(any())
        } throws(Exception("SOAP fault"))

        val personClient = PersonClient(personV3)
        val result = personClient.personInfo(AktørId("123456789"))

        verify(exactly = 1) {
            personV3.hentPerson(any())
        }

        when (result) {
            is OppslagResult.Feil -> {
                when (result.feil) {
                    is Feil.Exception -> assertEquals("SOAP fault", (result.feil as Feil.Exception).feilmelding)
                    else -> fail { "Expected Feil.Exception to be returned" }
                }
            }
            else -> fail { "Expected OppslagResult.Feil to be returned" }
        }
    }

    @Test
    fun `personInfo skal få person når oppslag er vellykket`() {
        val personV3 = mockk<PersonV3>()

        val aktørId = AktørId("123456789")

        every {
            personV3.hentPerson(match { actualRequest ->
                (actualRequest.aktoer is AktoerId)
                        && (actualRequest.aktoer as AktoerId).aktoerId == aktørId.aktor
                        && actualRequest.informasjonsbehov.size == 1
                        && actualRequest.informasjonsbehov.contains(Informasjonsbehov.ADRESSE)
            })
        } returns HentPersonResponse().apply {
            person = no.nav.tjeneste.virksomhet.person.v3.informasjon.Person().apply {
                aktoer = AktoerId().apply {
                    aktoerId = aktørId.aktor
                }
                personnavn = Personnavn().apply {
                    fornavn = "Bjarne"
                    mellomnavn = null
                    etternavn = "Betjent"
                }
                foedselsdato = Foedselsdato().apply {
                    foedselsdato = LocalDate.of(2018, 11, 19).toXmlGregorianCalendar()
                }
                kjoenn = Kjoenn().apply {
                    kjoenn = Kjoennstyper().apply {
                        value = "M"
                    }
                }
                bostedsadresse = Bostedsadresse().apply {
                    strukturertAdresse = Gateadresse().apply {
                        poststed = Postnummer().apply {
                            value = "0557"
                        }
                        landkode = Landkoder().apply {
                            value = "NOR"
                        }
                        tilleggsadresse = "Offisiell adresse"
                        kommunenummer = "0301"
                        gatenavn = "SANNERGATA"
                        husnummer = 2
                    }
                }
            }
        }

        val personClient = PersonClient(personV3)
        val result = personClient.personInfo(aktørId)

        verify(exactly = 1) {
            personV3.hentPerson(any())
        }

        val expected = Person(
                id = aktørId,
                fornavn = "Bjarne",
                etternavn = "Betjent",
                fdato = LocalDate.of(2018, 11, 19),
                kjønn = Kjønn.MANN,
                bostedsland = "NOR"
        )

        when (result) {
            is OppslagResult.Ok -> assertEquals(expected, result.data)
            else -> fail { "Expected OppslagResult.Ok to be returned" }
        }
    }

    @Test
    fun `geografiskTilknytning skal få feil når tjenesten kaster exception`() {
        val personV3 = mockk<PersonV3>()

        every {
            personV3.hentGeografiskTilknytning(any())
        } throws(Exception("SOAP fault"))

        val personClient = PersonClient(personV3)
        val result = personClient.geografiskTilknytning(AktørId("123456789"))

        verify(exactly = 1) {
            personV3.hentGeografiskTilknytning(any())
        }

        when (result) {
            is OppslagResult.Feil -> {
                when (result.feil) {
                    is Feil.Exception -> assertEquals("SOAP fault", (result.feil as Feil.Exception).feilmelding)
                    else -> fail { "Expected Feil.Exception to be returned" }
                }
            }
            else -> fail { "Expected OppslagResult.Feil to be returned" }
        }
    }

    @Test
    fun `geografiskTilknytning skal få person når oppslag er vellykket`() {
        val personV3 = mockk<PersonV3>()

        val aktørId = AktørId("123456789")

        every {
            personV3.hentGeografiskTilknytning(match { actualRequest ->
                (actualRequest.aktoer is AktoerId)
                        && (actualRequest.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } returns HentGeografiskTilknytningResponse().apply {
            aktoer = AktoerId().apply {
                aktoerId = aktørId.aktor
            }
            navn = Personnavn().apply {
                etternavn = "BLYANT"
                mellomnavn = "SMEKKER"
                sammensattNavn = "BLYANT SMEKKER"
            }
            geografiskTilknytning = Bydel().apply {
                geografiskTilknytning = "030103"
            }
        }

        val personClient = PersonClient(personV3)
        val result = personClient.geografiskTilknytning(aktørId)

        verify(exactly = 1) {
            personV3.hentGeografiskTilknytning(any())
        }

        val expected = GeografiskTilknytning(null, GeografiskOmraade("BYDEL", "030103"))

        when (result) {
            is OppslagResult.Ok -> assertEquals(expected, result.data)
            else -> fail { "Expected OppslagResult.Ok to be returned" }
        }
    }
}
