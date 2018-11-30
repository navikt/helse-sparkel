package no.nav.helse

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class EnvironmentTest {

    @Test
    fun `property should be resolved when accessed`() {
        val env = Environment(mapOf(
                "SECURITY_TOKEN_SERVICE_URL" to "http://example"
        ))

        Assertions.assertEquals("http://example", env.securityTokenServiceEndpointUrl)
    }

    @Test
    fun `property should throw if not set when accessed`() {
        val env = Environment(mapOf())

        try {
            env.securityTokenServiceEndpointUrl
            fail("RuntimeException should be thrown when accessing a property which is not set")
        } catch (e: RuntimeException) {
            // expected
        }
    }
}
