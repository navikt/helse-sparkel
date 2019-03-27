package no.nav.helse.ws.organisasjon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class Mod11Test {

    @Test
    fun `bokstaver er ikke lov`() {
        assertThrows(IllegalArgumentException::class.java) {
            Mod11.kontrollsiffer("123456789B")
        }
    }

    @Test
    fun `for kontonummer`() {
        assertEquals('3', Mod11.kontrollsiffer("1234567890"))
    }

    @Test
    fun `for organisasjonsnummer`() {
        assertEquals('2', Mod11.kontrollsiffer("88964078"))
        assertEquals('7', Mod11.kontrollsiffer("98388745"))
        assertEquals('0', Mod11.kontrollsiffer("99527767"))
    }
}
