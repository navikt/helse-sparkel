package no.nav.helse

import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OppslagResultTest {

    private val okVal = OppslagResult.Ok("OK")
    private val feilVal = OppslagResult.Feil(HttpStatusCode.NotFound, Feil.Feilmelding("Feil"))

    @Test
    fun `høyreverdi kan mappes til ny verdi`() {
        val newVal = 1234
        assertEquals(OppslagResult.Ok(newVal), okVal.map { newVal })
    }

    @Test
    fun `venstreverdi kan ikke mappes til ny verdi`() {
        val newVal = 1234
        assertEquals(feilVal, feilVal.map { newVal })
    }

    @Test
    fun `høyreverdi kan flatmappes til ny type`() {
        assertEquals(feilVal, okVal.flatMap { feilVal })
    }

    @Test
    fun `venstreverdi kan ikke flatmappes til ny type`() {
        assertEquals(feilVal, feilVal.flatMap { okVal })
    }

    @Test
    fun `fold skal velge riktig callback når verdi er venstreverdi`() {
        val newVal = "Hello, World"
        assertEquals(newVal, feilVal.fold( { newVal }, { "This should not return" }))
    }

    @Test
    fun `fold skal velge riktig callback når verdi er høyreverdi`() {
        val newVal = "Hello, World"
        assertEquals(newVal, okVal.fold( { "This should not return" }, { newVal }))
    }

    @Test
    fun `orElse skal gi høyreverdi når den er tilstede`() {
        assertEquals(okVal.data, okVal.orElse { "This should not return" })
    }

    @Test
    fun `orElse skal gi alternativ verdi når venstreverdi er tilstede`() {
        val newVal = "Hello, World"
        assertEquals(newVal, feilVal.orElse { newVal })
    }
}