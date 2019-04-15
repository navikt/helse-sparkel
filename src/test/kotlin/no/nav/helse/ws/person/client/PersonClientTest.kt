package no.nav.helse.ws.person.client

import arrow.core.Try
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.common.toLocalDate
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
            is Try.Failure -> assertEquals("SOAP fault", result.exception.message)
            else -> fail { "Expected Try.Failure to be returned" }
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

        when (result) {
            is Try.Success -> {
                assertEquals(aktørId.aktor, (result.value.aktoer as AktoerId).aktoerId)
                assertEquals("Bjarne", result.value.personnavn.fornavn)
                assertEquals("Betjent", result.value.personnavn.etternavn)
                assertEquals(LocalDate.of(2018, 11, 19), result.value.foedselsdato.foedselsdato.toLocalDate())
                assertEquals("M", result.value.kjoenn.kjoenn.value)
                assertEquals("NOR", result.value.bostedsadresse.strukturertAdresse.landkode.value)
            }
            else -> fail { "Expected Try.Success to be returned" }
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
            is Try.Failure -> assertEquals("SOAP fault", result.exception.message)
            else -> fail { "Expected Try.Failure to be returned" }
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

        when (result) {
            is Try.Success -> {
                assertEquals(aktørId.aktor, (result.value.aktoer as AktoerId).aktoerId)
                assertEquals("SMEKKER", result.value.navn.mellomnavn)
                assertEquals("BLYANT", result.value.navn.etternavn)
                assertEquals("BLYANT SMEKKER", result.value.navn.sammensattNavn)
                assertTrue(result.value.geografiskTilknytning is Bydel)
                assertEquals("030103", result.value.geografiskTilknytning.geografiskTilknytning)
            }
            else -> fail { "Expected Try.Success to be returned" }
        }
    }
}
