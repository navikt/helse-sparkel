package no.nav.helse.ws.person

import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class PersonMapperTest {

    @Test
    fun personMappingMann() {
        val tpsMann = mannResponse()
        val id = Fødselsnummer("12345678910")
        val expected = no.nav.helse.ws.person.Person(id = id,
                fornavn = "Bjarne",
                etternavn = "Betjent",
                kjønn = Kjønn.MANN)
        val actual = PersonMapper.toPerson(id, tpsMann)
        assertEquals(expected, actual)
    }

    @Test
    fun personMappingKvinne() {
        val tpsKvinne = kvinneResponse()
        val id = Fødselsnummer("12345678910")
        val expected = no.nav.helse.ws.person.Person(id = id,
                fornavn = "Leonora",
                mellomnavn = "Dorothea",
                etternavn = "Dahl",
                kjønn = Kjønn.KVINNE)
        val actual = PersonMapper.toPerson(id, tpsKvinne)
        assertEquals(expected, actual)
    }

    private fun mannResponse(): HentPersonResponse {
        val mannen = Person().apply {
            personnavn = Personnavn().apply {
                fornavn = "Bjarne"
                etternavn = "Betjent"
                kjoenn = Kjoenn().apply {
                    kjoenn = Kjoennstyper().apply {
                        value = "M"
                    }
                }
            }
        }

        return HentPersonResponse().apply { person = mannen }
    }

    private fun kvinneResponse(): HentPersonResponse {
        val kvinnen = Person().apply {
            personnavn = Personnavn().apply {
                fornavn = "Leonora"
                mellomnavn = "Dorothea"
                etternavn = "Dahl"
                kjoenn = Kjoenn().apply {
                    kjoenn = Kjoennstyper().apply {
                        value = "K"
                    }
                }
            }
        }

        return HentPersonResponse().apply { person = kvinnen }
    }

}