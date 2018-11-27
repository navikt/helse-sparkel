package no.nav.helse.ws.person

import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.WSHentPersonResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.xml.datatype.DatatypeFactory

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

    private fun mannResponse(): WSHentPersonResponse {
        val mannen = WSPerson().apply {
            personnavn = WSPersonnavn().apply {
                fornavn = "Bjarne"
                etternavn = "Betjent"
                kjoenn = WSKjoenn().apply {
                    kjoenn = WSKjoennstyper().apply {
                        value = "M"
                    }
                }
            }
            foedselsdato = WSFoedselsdato().apply {
                foedselsdato = DatatypeFactory.newInstance().newXMLGregorianCalendar().apply {
                    year = 2018
                    month = 11
                    day = 20
                }
            }
        }

        return WSHentPersonResponse().apply { person = mannen }
    }

    private fun kvinneResponse(): WSHentPersonResponse {
        val kvinnen = WSPerson().apply {
            personnavn = WSPersonnavn().apply {
                fornavn = "Leonora"
                mellomnavn = "Dorothea"
                etternavn = "Dahl"
                kjoenn = WSKjoenn().apply {
                    kjoenn = WSKjoennstyper().apply {
                        value = "K"
                    }
                }
                foedselsdato = WSFoedselsdato().apply {
                    foedselsdato = DatatypeFactory.newInstance().newXMLGregorianCalendar().apply {
                        year = 2018
                        month = 11
                        day = 19
                    }
                }
            }
        }

        return WSHentPersonResponse().apply { person = kvinnen }
    }

}
