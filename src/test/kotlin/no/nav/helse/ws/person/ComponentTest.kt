package no.nav.helse.ws.person

import io.prometheus.client.CollectorRegistry
import no.nav.helse.Failure
import no.nav.helse.Success
import no.nav.helse.ws.Fødselsnummer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate

class ComponentTest {

    private val metricsRegistry = CollectorRegistry.defaultRegistry

    @AfterEach
    fun afterEach() {
        metricsRegistry.clear()
    }

    @Test
    fun stubbedLookup() {
        val personClient = PersonClient(PersonV3Stub())
        val expected = Person(
                id = Fødselsnummer("12345678910"),
                fornavn = "Bjarne",
                etternavn = "Betjent",
                fdato = LocalDate.of(2018, 11, 19),
                kjønn = Kjønn.MANN
        )
        val actual = personClient.personInfo(Fødselsnummer("12345678910"))
        when (actual) {
            is Success<*> -> {
                assertEquals(1.0, metricsRegistry.getSampleValue(
                        "oppslag_person", arrayOf("status"), arrayOf("success")))
                assertEquals(expected, actual.data)
            }
            is Failure -> fail { "This lookup was expected to succeed, but it didn't" }
        }
    }

    @Test
    fun stubbedLookupWithError() {
        val personClient = PersonClient(PersonV3MisbehavingStub())
        val expected = Failure(listOf("SOAPy stuff got besmirched"))
        val actual = personClient.personInfo(Fødselsnummer("12345678910"))
        when (actual) {
            is Success<*> -> fail { "This lookup was expected to fail, but it didn't" }
            is Failure -> {
                assertEquals(1.0, metricsRegistry.getSampleValue(
                        "oppslag_person", arrayOf("status"), arrayOf("failure")))
                assertEquals(expected, actual)
            }
        }
    }

}
