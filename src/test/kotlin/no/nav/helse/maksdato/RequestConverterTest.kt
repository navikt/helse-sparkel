package no.nav.helse.maksdato

import org.json.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.*

class RequestConverterTest {

    @Test
    fun `valid json is converted`() {
        val expected = Success(MaksdatoRequest(
                førsteFraværsdag = LocalDate.parse("2019-02-01"),
                førsteSykepengedag = LocalDate.parse("2019-02-20"),
                personensAlder = 24,
                tidligerePerioder = listOf(
                        Tidsperiode(LocalDate.parse("2018-12-03"), LocalDate.parse("2018-12-21"))
                ))
        )
        val actual = JSONObject(jsonMedEnPeriode).toMaksdatoRequest()
        assertEquals(expected, actual)
    }

    @Test
    fun `invalid json is rejected`() {
        assertTrue(JSONObject(bogusJson).toMaksdatoRequest() is Failure)
    }

    private val jsonMedEnPeriode = """
    {
      "førsteFraværsdag": "2019-02-01",
      "førsteSykepengedag": "2019-02-20",
      "personensAlder": 24,
      "tidligerePerioder": [
        {"fom": "2018-12-03", "tom": "2018-12-21"}
      ]
    }
    """.trimIndent()

    private val bogusJson = """{"blabla": "stuff"}"""

}

