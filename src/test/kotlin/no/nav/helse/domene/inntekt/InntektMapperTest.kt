package no.nav.helse.domene.inntekt

import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.inntekt.domain.Inntekt
import no.nav.helse.domene.inntekt.domain.Virksomhet
import no.nav.helse.domene.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class InntektMapperTest {

    @Test
    fun `skal mappe forskjellige inntektstyper`() {
        val aktørId = AktørId("11987654321")
        val tom = YearMonth.now()
        val fom = tom.minusMonths(1)

        val expected = listOf(
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(2500)),
                Inntekt.Ytelse(Virksomhet.Organisasjon(Organisasjonsnummer("995277670")),
                        fom, BigDecimal.valueOf(500), "barnetrygd"),
                Inntekt.Næring(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(1500), "næringsinntekt"),
                Inntekt.PensjonEllerTrygd(Virksomhet.Organisasjon(Organisasjonsnummer("995277670")),
                        fom, BigDecimal.valueOf(3000), "alderspensjon")
        )

        val given = listOf(Loennsinntekt().apply {
                beloep = BigDecimal.valueOf(2500)
            inntektsmottaker = AktoerId().apply {
                aktoerId = aktørId.aktor
            }
            virksomhet = Organisasjon().apply {
                orgnummer = "889640782"
            }
            utbetaltIPeriode = fom.toXmlGregorianCalendar()
        }, YtelseFraOffentlige().apply {
            beloep = BigDecimal.valueOf(500)
            inntektsmottaker = AktoerId().apply {
                aktoerId = aktørId.aktor
            }
            virksomhet = Organisasjon().apply {
                orgnummer = "995277670"
            }
            utbetaltIPeriode = fom.toXmlGregorianCalendar()
            beskrivelse = YtelseFraOffentligeBeskrivelse().apply {
                value = "barnetrygd"
            }
        }, Naeringsinntekt().apply {
            beloep = BigDecimal.valueOf(1500)
            inntektsmottaker = AktoerId().apply {
                aktoerId = aktørId.aktor
            }
            virksomhet = Organisasjon().apply {
                orgnummer = "889640782"
            }
            utbetaltIPeriode = fom.toXmlGregorianCalendar()
            beskrivelse = Naeringsinntektsbeskrivelse().apply {
                value = "næringsinntekt"
            }
        }, PensjonEllerTrygd().apply {
            beloep = BigDecimal.valueOf(3000)
            inntektsmottaker = AktoerId().apply {
                aktoerId = aktørId.aktor
            }
            virksomhet = Organisasjon().apply {
                orgnummer = "995277670"
            }
            utbetaltIPeriode = fom.toXmlGregorianCalendar()
            beskrivelse = PensjonEllerTrygdebeskrivelse().apply {
                value = "alderspensjon"
            }
        })

        val actual = given.map { givenInntekt ->
            InntektMapper.mapToIntekt(givenInntekt)
        }

        assertEquals(expected.size, actual.size)
        expected.forEachIndexed { key, inntekt ->
            assertEquals(inntekt, actual[key])
        }
    }
}
