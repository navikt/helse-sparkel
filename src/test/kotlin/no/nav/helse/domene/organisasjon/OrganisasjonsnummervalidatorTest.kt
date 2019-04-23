package no.nav.helse.domene.organisasjon

import no.nav.helse.domene.organisasjon.Organisasjonsnummervalidator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrganisasjonsnummervalidatorTest {
    @Test
    fun `ugyldig organisasjonsnummer`() {
        assertFalse(Organisasjonsnummervalidator.erGyldig("88964078"))
        assertFalse(Organisasjonsnummervalidator.erGyldig("889640781"))
        assertFalse(Organisasjonsnummervalidator.erGyldig("foofoofof"))
    }

    @Test
    fun `gyldig organisasjonsnummer`() {
        assertTrue(Organisasjonsnummervalidator.erGyldig("889640782"))
    }
}
