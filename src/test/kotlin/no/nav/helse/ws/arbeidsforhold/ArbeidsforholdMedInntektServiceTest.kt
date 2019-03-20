package no.nav.helse.ws.arbeidsforhold

import io.mockk.every
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.helse.Either
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.inntekt.Inntekt
import no.nav.helse.ws.inntekt.InntektService
import no.nav.helse.ws.inntekt.Opptjeningsperiode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class ArbeidsforholdMedInntektServiceTest {

    @Test
    fun `skal sammenstille arbeidsforhold og inntekter`() {
        val aktørId = AktørId("123456789")
        val fom = LocalDate.parse("2019-01-01")
        val tom = LocalDate.parse("2019-03-01")

        val arbeidsforholdliste = listOf(
                Arbeidsforhold(Arbeidsgiver.Organisasjon("22334455", "S. VINDEL & SØNN"), fom),
                Arbeidsforhold(Arbeidsgiver.Organisasjon("66778899", "Matbutikken A/S"), LocalDate.parse("2018-01-01"), fom)
        )

        val inntekter = listOf(
                Inntekt(no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon("22334455"), Opptjeningsperiode(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-31")), BigDecimal(20000)),
                Inntekt(no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon("22334455"), Opptjeningsperiode(LocalDate.parse("2019-02-01"), LocalDate.parse("2019-02-28")), BigDecimal(25000)),
                Inntekt(no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon("66778899"), Opptjeningsperiode(LocalDate.parse("2018-10-01"), LocalDate.parse("2018-10-31")), BigDecimal(15000)),
                Inntekt(no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon("66778899"), Opptjeningsperiode(LocalDate.parse("2018-11-01"), LocalDate.parse("2018-11-30")), BigDecimal(16000)),
                Inntekt(no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon("66778899"), Opptjeningsperiode(LocalDate.parse("2018-12-01"), LocalDate.parse("2018-12-31")), BigDecimal(17000))
        )

        val expected = listOf(
                ArbeidsforholdMedInntekt(Arbeidsforhold(Arbeidsgiver.Organisasjon("22334455", "S. VINDEL & SØNN"), fom), listOf(
                        InntektUtenArbeidsgiver(Opptjeningsperiode(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-31")), BigDecimal(20000)),
                        InntektUtenArbeidsgiver(Opptjeningsperiode(LocalDate.parse("2019-02-01"), LocalDate.parse("2019-02-28")), BigDecimal(25000))
                )),
                ArbeidsforholdMedInntekt(Arbeidsforhold(Arbeidsgiver.Organisasjon("66778899", "Matbutikken A/S"), LocalDate.parse("2018-01-01"), fom), listOf(
                        InntektUtenArbeidsgiver(Opptjeningsperiode(LocalDate.parse("2018-10-01"), LocalDate.parse("2018-10-31")), BigDecimal(15000)),
                        InntektUtenArbeidsgiver(Opptjeningsperiode(LocalDate.parse("2018-11-01"), LocalDate.parse("2018-11-30")), BigDecimal(16000)),
                        InntektUtenArbeidsgiver(Opptjeningsperiode(LocalDate.parse("2018-12-01"), LocalDate.parse("2018-12-31")), BigDecimal(17000))
                ))
        )

        val arbeidsforholdService = mockk<ArbeidsforholdService>()
        val inntektService = mockk<InntektService>()

        val aktiveArbeidsforholdService = ArbeidsforholdMedInntektService(arbeidsforholdService, inntektService)

        val arbeidsforholdAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals")
        val inntektAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(arbeidsforholdliste)

        every {
            inntektService.hentInntekter(aktørId, YearMonth.from(fom), YearMonth.from(tom))
        } returns Either.Right(inntekter)

        val actual = aktiveArbeidsforholdService.finnArbeidsforholdMedInntekter(aktørId, fom, tom)

        val arbeidsforholdAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals")
        val inntektAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        assertEquals(0.0, arbeidsforholdAvviksCounterAfter - arbeidsforholdAvviksCounterBefore)
        assertEquals(0.0, inntektAvviksCounterAfter - inntektAvviksCounterBefore)

        when (actual) {
            is Either.Right -> {
                assertEquals(2, actual.right.size)
                assertEquals(expected, actual.right)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal telle inntekter som ikke har et tilhørende arbeidsforhold`() {
        val aktørId = AktørId("123456789")
        val fom = LocalDate.parse("2019-01-01")
        val tom = LocalDate.parse("2019-03-01")

        val arbeidsforholdliste = listOf(
                Arbeidsforhold(Arbeidsgiver.Organisasjon("22334455", "S. VINDEL & SØNN"), fom)
        )

        val inntekter = listOf(
                Inntekt(no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon("22334455"), Opptjeningsperiode(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-31")), BigDecimal(20000)),
                Inntekt(no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon("22334455"), Opptjeningsperiode(LocalDate.parse("2019-02-01"), LocalDate.parse("2019-02-28")), BigDecimal(25000)),
                Inntekt(no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon("66778899"), Opptjeningsperiode(LocalDate.parse("2018-10-01"), LocalDate.parse("2018-10-31")), BigDecimal(15000)),
                Inntekt(no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon("66778899"), Opptjeningsperiode(LocalDate.parse("2018-11-01"), LocalDate.parse("2018-11-30")), BigDecimal(16000)),
                Inntekt(no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon("66778899"), Opptjeningsperiode(LocalDate.parse("2018-12-01"), LocalDate.parse("2018-12-31")), BigDecimal(17000))
        )

        val expected = listOf(
                ArbeidsforholdMedInntekt(Arbeidsforhold(Arbeidsgiver.Organisasjon("22334455", "S. VINDEL & SØNN"), fom), listOf(
                        InntektUtenArbeidsgiver(Opptjeningsperiode(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-31")), BigDecimal(20000)),
                        InntektUtenArbeidsgiver(Opptjeningsperiode(LocalDate.parse("2019-02-01"), LocalDate.parse("2019-02-28")), BigDecimal(25000))
                ))
        )

        val arbeidsforholdService = mockk<ArbeidsforholdService>()
        val inntektService = mockk<InntektService>()

        val aktiveArbeidsforholdService = ArbeidsforholdMedInntektService(arbeidsforholdService, inntektService)

        val arbeidsforholdAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals")
        val inntektAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(arbeidsforholdliste)

        every {
            inntektService.hentInntekter(aktørId, YearMonth.from(fom), YearMonth.from(tom))
        } returns Either.Right(inntekter)

        val actual = aktiveArbeidsforholdService.finnArbeidsforholdMedInntekter(aktørId, fom, tom)

        val arbeidsforholdAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals")
        val inntektAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        assertEquals(0.0, arbeidsforholdAvviksCounterAfter - arbeidsforholdAvviksCounterBefore)
        assertEquals(3.0, inntektAvviksCounterAfter - inntektAvviksCounterBefore)

        when (actual) {
            is Either.Right -> {
                assertEquals(1, actual.right.size)
                assertEquals(expected, actual.right)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal telle arbeidsforhold som ikke ha tilhørende inntekter`() {
        val aktørId = AktørId("123456789")
        val fom = LocalDate.parse("2019-01-01")
        val tom = LocalDate.parse("2019-03-01")

        val arbeidsforholdliste = listOf(
                Arbeidsforhold(Arbeidsgiver.Organisasjon("22334455", "S. VINDEL & SØNN"), fom),
                Arbeidsforhold(Arbeidsgiver.Organisasjon("66778899", "Matbutikken A/S"), LocalDate.parse("2018-01-01"), fom)
        )

        val inntekter = listOf(
                Inntekt(no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon("66778899"), Opptjeningsperiode(LocalDate.parse("2018-10-01"), LocalDate.parse("2018-10-31")), BigDecimal(15000)),
                Inntekt(no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon("66778899"), Opptjeningsperiode(LocalDate.parse("2018-11-01"), LocalDate.parse("2018-11-30")), BigDecimal(16000)),
                Inntekt(no.nav.helse.ws.inntekt.Arbeidsgiver.Organisasjon("66778899"), Opptjeningsperiode(LocalDate.parse("2018-12-01"), LocalDate.parse("2018-12-31")), BigDecimal(17000))
        )

        val expected = listOf(
                ArbeidsforholdMedInntekt(Arbeidsforhold(Arbeidsgiver.Organisasjon("66778899", "Matbutikken A/S"), LocalDate.parse("2018-01-01"), fom), listOf(
                        InntektUtenArbeidsgiver(Opptjeningsperiode(LocalDate.parse("2018-10-01"), LocalDate.parse("2018-10-31")), BigDecimal(15000)),
                        InntektUtenArbeidsgiver(Opptjeningsperiode(LocalDate.parse("2018-11-01"), LocalDate.parse("2018-11-30")), BigDecimal(16000)),
                        InntektUtenArbeidsgiver(Opptjeningsperiode(LocalDate.parse("2018-12-01"), LocalDate.parse("2018-12-31")), BigDecimal(17000))
                ))
        )

        val arbeidsforholdService = mockk<ArbeidsforholdService>()
        val inntektService = mockk<InntektService>()

        val aktiveArbeidsforholdService = ArbeidsforholdMedInntektService(arbeidsforholdService, inntektService)

        val arbeidsforholdAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals")
        val inntektAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(arbeidsforholdliste)

        every {
            inntektService.hentInntekter(aktørId, YearMonth.from(fom), YearMonth.from(tom))
        } returns Either.Right(inntekter)

        val actual = aktiveArbeidsforholdService.finnArbeidsforholdMedInntekter(aktørId, fom, tom)

        val arbeidsforholdAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals")
        val inntektAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        assertEquals(1.0, arbeidsforholdAvviksCounterAfter - arbeidsforholdAvviksCounterBefore)
        assertEquals(0.0, inntektAvviksCounterAfter - inntektAvviksCounterBefore)

        when (actual) {
            is Either.Right -> {
                assertEquals(1, actual.right.size)
                assertEquals(expected, actual.right)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }
}
