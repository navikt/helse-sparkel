package no.nav.helse.ws.organisasjon

import io.ktor.http.HttpStatusCode
import io.prometheus.client.CollectorRegistry
import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ComponentTest {

    private val metricsRegistry = CollectorRegistry.defaultRegistry

    @AfterEach
    fun afterEach() {
        metricsRegistry.clear()
    }

    @Test
    fun stubbedLookup() {
        val organisasjonClient = OrganisasjonClient(OrganisasjonV5Stub())
        val expected = OrganisasjonResponse("fornavn, mellomnavn, etternavn")
        val actual = organisasjonClient.hentOrganisasjon(
            orgnr = OrganisasjonsNummer("12345")
        )
        when (actual) {
            is OppslagResult.Ok -> assertEquals(expected, actual.data)
            is OppslagResult.Feil -> fail { "This lookup was expected to succeed, but it didn't" }
        }
    }

    @Test
    fun stubbedLookupWithError() {
        val organisasjonClient = OrganisasjonClient(OrganisasjonV5MisbehavingStub())
        val expected = OppslagResult.Feil(HttpStatusCode.InternalServerError, Feil.Exception("SOAPy stuff got besmirched", Exception("SOAPy stuff got besmirched")))
        val actual = organisasjonClient.hentOrganisasjon(
                orgnr = OrganisasjonsNummer("12345")
        )
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
