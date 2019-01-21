package no.nav.helse.ws.person

import no.nav.helse.ws.*
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*
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
                kjønn = Kjønn.MANN)
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
                kjønn = Kjønn.KVINNE)
        val actual = PersonMapper.toPerson(tpsResponse)
        assertEquals(expected, actual)
    }

    @Test
    fun personHistorikk() {
        val tpsHistorikk = historikkResponse()
        val expected = Personhistorikk(
                AktørId("1234567891011"),
                emptyList(),
                emptyList(),
                listOf(
                   TidsperiodeMedVerdi("veien 2A, Poststedet", LocalDate.of(2019, 1, 21),
                           LocalDate.of(2019, 1, 22))
                )
        )
        val actual = PersonhistorikkMapper.toPersonhistorikk(tpsHistorikk)
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
        }

        return HentPersonResponse().apply { person = kvinnen }
    }

    private fun historikkResponse(): HentPersonhistorikkResponse {
        val bostedsadresse = Bostedsadresse().apply {
            strukturertAdresse = Gateadresse().apply {
                gatenavn = "veien"
                husnummer = 2
                husbokstav = "A"
                withPoststed(Postnummer().apply { value = "Poststedet" })
            }
        }
        return HentPersonhistorikkResponse().apply {
            aktoer = AktoerId().apply { aktoerId = "1234567891011" }
            val periode = BostedsadressePeriode().apply {
                periode = Periode()
                        .withFom(LocalDate.of(2019, 1, 21).toXmlGregorianCalendar())
                        .withTom(LocalDate.of(2019, 1, 22).toXmlGregorianCalendar())
                withBostedsadresse(bostedsadresse)
            }
            withBostedsadressePeriodeListe(periode)
        }
    }

}