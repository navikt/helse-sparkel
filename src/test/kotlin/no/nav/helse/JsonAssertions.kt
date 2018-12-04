package no.nav.helse

import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions

fun assertJsonEquals(expected: JSONObject, actual: JSONObject) {
    Assertions.assertEquals(expected.length(), actual.length())

    expected.keys().forEach {
        Assertions.assertTrue(actual.has(it))

        val expectedValue = expected.get(it)
        when(expectedValue) {
            is JSONObject -> assertJsonEquals(expectedValue, actual.get(it) as JSONObject)
            is JSONArray -> assertJsonEquals(expectedValue, actual.get(it) as JSONArray)
            else -> Assertions.assertEquals(expectedValue, actual.get(it))
        }
    }
}

fun assertJsonEquals(expected: JSONArray, actual: JSONArray) {
    Assertions.assertEquals(expected.length(), actual.length())

    expected.forEachIndexed { index, it ->
        when(it) {
            is JSONObject -> assertJsonEquals(it, actual.get(index) as JSONObject)
            is JSONArray -> assertJsonEquals(it, actual.get(index) as JSONArray)
            else -> Assertions.assertEquals(it, actual.get(index))
        }
    }
}
