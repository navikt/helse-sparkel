package no.nav.helse.domene.aiy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CombineTest {

    @Test
    fun `match and note diff`() {
        val letters = listOf('a')
        val strings = listOf("apple")

        val expected = Triple(
                first = mapOf(
                        'a' to listOf("apple")
                ),
                second = emptyList<Char>(),
                third = emptyList<String>()
        )

        assertEquals(expected, letters.combineAndComputeDiff(strings) { char, string ->
            string[0] == char
        })
    }

    @Test
    fun `match and note diff with same element`() {
        val letters = listOf('a', 'a')
        val strings = listOf("apple")

        val expected = Triple(
                first = mapOf(
                        'a' to listOf("apple")
                ),
                second = listOf('a'),
                third = emptyList<String>()
        )

        assertEquals(expected, letters.combineAndComputeDiff(strings) { char, string ->
            string[0] == char
        })
    }

    @Test
    fun `match and note diff without matches`() {
        val letters = listOf('a')
        val strings = listOf("banana")

        val expected = Triple(
                first = emptyMap<Char, List<String>>(),
                second = listOf('a'),
                third = listOf("banana")
        )

        assertEquals(expected, letters.combineAndComputeDiff(strings) { char, string ->
            string[0] == char
        })
    }

    @Test
    fun `match and note diff with duplicates`() {
        val letters = listOf('a', 'b', 'c', 'a', 'c')
        val strings = listOf("banana", "avocado", "apple", "apple", "pear")

        val expected = Triple(
                first = mapOf(
                        'a' to listOf("avocado", "apple", "apple"),
                        'b' to listOf("banana")
                ),
                second = listOf('c', 'a', 'c'),
                third = listOf("pear")
        )

        assertEquals(expected, letters.combineAndComputeDiff(strings) { char, string ->
            string[0] == char
        })
    }
}


