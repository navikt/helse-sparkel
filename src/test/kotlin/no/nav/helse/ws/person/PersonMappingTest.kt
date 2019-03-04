package no.nav.helse.ws.person

import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Foedselsdato
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoenn
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoennstyper
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.xml.datatype.DatatypeFactory

class PersonMappingTest {

    @Test
    fun personMappingMann() {
        val tpsMann = mannResponse()
        val id = AktørId("1234567891011")
        val expected = no.nav.helse.ws.person.Person(
                id = id,
                fornavn = "Bjarne",
                etternavn = "Betjent",
                fdato = LocalDate.of(2018, 11, 20),
                kjønn = Kjønn.MANN,
                bostedsland = "NOR")
        val actual = PersonMapper.toPerson(tpsMann)
        assertEquals(expected, actual)
    }

    @Test
    fun personMappingKvinne() {
        val tpsResponse = kvinneResponse()
        val id = AktørId("1234567891011")
        val expected = no.nav.helse.ws.person.Person(
                id = id,
                fornavn = "Leonora",
                mellomnavn = "Dorothea",
                etternavn = "Dahl",
                fdato = LocalDate.of(2018, 11, 19),
                kjønn = Kjønn.KVINNE,
                bostedsland = "NOR")
        val actual = PersonMapper.toPerson(tpsResponse)
        assertEquals(expected, actual)
    }

    private fun mannResponse(): Person {
        return Person().apply {
            personnavn = Personnavn().apply {
                fornavn = "Bjarne"
                etternavn = "Betjent"
                kjoenn = Kjoenn().apply {
                    kjoenn = Kjoennstyper().apply {
                        value = "M"
                    }
                }
                aktoer = AktoerId().apply {
                    aktoerId = "1234567891011"
                }
            }
            foedselsdato = Foedselsdato().apply {
                foedselsdato = DatatypeFactory.newInstance().newXMLGregorianCalendar().apply {
                    year = 2018
                    month = 11
                    day = 20
                }
            }
            bostedsadresse = Bostedsadresse().apply {
                strukturertAdresse = Gateadresse().apply {
                    landkode = Landkoder().apply {
                        value = "NOR"
                    }
                }
            }
        }
    }

    private fun kvinneResponse(): Person {
        return Person().apply {
            personnavn = Personnavn().apply {
                fornavn = "Leonora"
                mellomnavn = "Dorothea"
                etternavn = "Dahl"
                kjoenn = Kjoenn().apply {
                    kjoenn = Kjoennstyper().apply {
                        value = "K"
                    }
                }
                aktoer = AktoerId().apply {
                    aktoerId = "1234567891011"
                }
                foedselsdato = Foedselsdato().apply {
                    foedselsdato = DatatypeFactory.newInstance().newXMLGregorianCalendar().apply {
                        year = 2018
                        month = 11
                        day = 19
                    }
                }
            }
            bostedsadresse = Bostedsadresse().apply {
                strukturertAdresse = Gateadresse().apply {
                    landkode = Landkoder().apply {
                        value = "NOR"
                    }
                }
            }
        }
    }

}
