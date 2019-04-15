package no.nav.helse.arrow

import arrow.core.Either
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class SequenceUTest {

    @Test
    fun `liste av either med bare høyreverdier skal gi høyreverdi`() {
        val list = listOf(
                Either.Right("One"),
                Either.Right("Two")).sequenceU()

        when (list) {
            is Either.Right -> {
                // ok
            }
            is Either.Left -> fail { "Expected Either.Right" }
        }
    }

    @Test
    fun `liste over either med en venstreverdi skal gi venstreverdi`() {
        val list = listOf(
                Either.Right("One"),
                Either.Right("Two"),
                Either.Left("Shit")).sequenceU()

        when (list) {
            is Either.Left -> {
                // ok
            }
            is Either.Right -> fail { "Expected Either.Left" }
        }
    }
}
