package no.nav.helse.ws

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.lang.IllegalArgumentException


class FødselsnummerTest {

    @Test
    fun mustBeNumbersOnly() {
        assertThrows(IllegalArgumentException::class.java) {
            Fødselsnummer("123456789AB")
        }
    }

    @Test
    fun cannotBeLessThanElevenDigits() {
        assertThrows(IllegalArgumentException::class.java) {
            Fødselsnummer("1234567890")
        }
    }

    @Test
    fun cannotBeMoreThanElevenDigits() {
        assertThrows(IllegalArgumentException::class.java) {
            Fødselsnummer("123456789101")
        }
    }

    @Test
    fun mustBeExactlyElevenDigits() {
        assertNotNull(Fødselsnummer("12345678910"))
    }

}