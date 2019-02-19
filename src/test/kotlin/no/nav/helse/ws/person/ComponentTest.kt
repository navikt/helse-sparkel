package no.nav.helse.ws.person

import io.ktor.http.HttpStatusCode
import io.prometheus.client.CollectorRegistry
import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.ws.AktørId
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
                id = AktørId("1234567891011"),
                fornavn = "Bjarne",
                etternavn = "Betjent",
                fdato = LocalDate.of(2018, 11, 19),
                kjønn = Kjønn.MANN,
                bostedsland = "NOR"
        )
        val actual = personClient.personInfo(AktørId("1234567891011"))
        when (actual) {
            is OppslagResult.Ok -> {
                assertEquals(expected, actual.data)
            }
            is OppslagResult.Feil -> fail { "This lookup was expected to succeed, but it didn't" }
        }
    }

    @Test
    fun stubbedLookupWithError() {
        val personClient = PersonClient(PersonV3MisbehavingStub())
        val expected = OppslagResult.Feil(HttpStatusCode.InternalServerError, Feil.Exception("SOAPy stuff got besmirched", Exception("SOAPy stuff got besmirched")))
        val actual = personClient.personInfo(AktørId("1234567891011"))
        when (actual) {
            is OppslagResult.Ok -> fail { "This lookup was expected to fail, but it didn't" }
            is OppslagResult.Feil -> {
                assertEquals(expected.httpCode, actual.httpCode)
                when (actual.feil) {
                    is Feil.Exception -> {
                        assertEquals(expected.feil.feilmelding, (actual.feil as Feil.Exception).feilmelding)
                        assertEquals(expected.feil.exception.message, (actual.feil as Feil.Exception).exception.message)
                    }
                    else -> fail { "Expected an exception to be returned" }
                }
            }
        }
    }

}
