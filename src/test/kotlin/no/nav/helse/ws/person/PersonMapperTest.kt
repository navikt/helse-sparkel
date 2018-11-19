package no.nav.helse.ws.person

import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.*
import javax.xml.datatype.*

class PersonMapperTest {

    @Test
    fun personMappingMann() {
        val tpsMann = mannResponse()
        val id = Fødselsnummer("12345678910")
        val expected = no.nav.helse.ws.person.Person(
                id = id,
                fornavn = "Bjarne",
                etternavn = "Betjent",
                fdato = LocalDate.of(2018, 11, 20),
                kjønn = Kjønn.MANN)
        val actual = PersonMapper.toPerson(id, tpsMann)
        assertEquals(expected, actual)
    }

    @Test
    fun personMappingKvinne() {
        val tpsKvinne = kvinneResponse()
        val id = Fødselsnummer("12345678910")
        val expected = no.nav.helse.ws.person.Person(
                id = id,
                fornavn = "Leonora",
                mellomnavn = "Dorothea",
                etternavn = "Dahl",
                fdato = LocalDate.of(2018, 11, 19),
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
            foedselsdato = Foedselsdato().apply {
                foedselsdato = DatatypeFactory.newInstance().newXMLGregorianCalendar().apply {
                    year = 2018
                    month = 11
                    day = 20
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
                foedselsdato = Foedselsdato().apply {
                    foedselsdato = DatatypeFactory.newInstance().newXMLGregorianCalendar().apply {
                        year = 2018
                        month = 11
                        day = 19
                    }
                }
            }
        }

        return HentPersonResponse().apply { person = kvinnen }
    }

}