package no.nav.helse.ws.person

import no.nav.helse.*
import no.nav.helse.ws.Fødselsnummer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class ComponentTest {

    @Test
    fun stubbedLookup() {
        val personClient = PersonClient(PersonV3Stub())
        val expected = Person(
                id = Fødselsnummer("12345678910"),
                fornavn = "Bjarne",
                etternavn = "Betjent",
                kjønn = Kjønn.MANN
        )
        val actual = personClient.personInfo(Fødselsnummer("12345678910"))
        when (actual) {
            is Success<*> -> assertEquals(expected, actual.data)
            is Failure -> fail { "PersonClient erred: ${actual.errors}" }
        }
    }

}