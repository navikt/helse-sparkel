package no.nav.helse.ws.aiy

import io.mockk.every
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.helse.Either
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdService
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsgiver
import no.nav.helse.ws.inntekt.InntektService
import no.nav.helse.ws.inntekt.domain.Inntekt
import no.nav.helse.ws.inntekt.domain.Virksomhet
import no.nav.helse.ws.organisasjon.domain.Organisasjon
import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class ArbeidInntektYtelseServiceTest {

    @Test
    fun `skal sammenstille arbeidsforhold og inntekter`() {
        val aktørId = AktørId("123456789")
        val fom = LocalDate.parse("2019-01-01")
        val tom = LocalDate.parse("2019-03-01")

        val arbeidsforholdliste = listOf(
                Arbeidsforhold(Arbeidsgiver.Virksomhet(Organisasjon.Virksomhet(Organisasjonsnummer("889640782"), "S. VINDEL & SØNN")), fom),
                Arbeidsforhold(Arbeidsgiver.Virksomhet(Organisasjon.Virksomhet(Organisasjonsnummer("995298775"), "Matbutikken A/S")), LocalDate.parse("2018-01-01"), fom)
        )

        val inntekter = listOf(
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")), YearMonth.of(2019, 1), BigDecimal(20000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")), YearMonth.of(2019, 2), BigDecimal(25000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), YearMonth.of(2018, 10), BigDecimal(15000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), YearMonth.of(2018, 11), BigDecimal(16000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), YearMonth.of(2018, 12), BigDecimal(17000))
        )

        val virksomhet1 = Organisasjonsnummer("889640782")
        val virksomhet2 = Organisasjonsnummer("995298775")
        val expected = ArbeidInntektYtelse(
                arbeidsforhold = mapOf(
                        Arbeidsforhold(Arbeidsgiver.Virksomhet(Organisasjon.Virksomhet(virksomhet1, "S. VINDEL & SØNN")), fom) to listOf(
                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 1), BigDecimal(20000)),
                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 2), BigDecimal(25000))
                ),
                Arbeidsforhold(Arbeidsgiver.Virksomhet(Organisasjon.Virksomhet(virksomhet2, "Matbutikken A/S")), LocalDate.parse("2018-01-01"), fom) to listOf(
                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 10), BigDecimal(15000)),
                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 11), BigDecimal(16000)),
                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 12), BigDecimal(17000))
                ))
        )

        val arbeidsforholdService = mockk<ArbeidsforholdService>()
        val inntektService = mockk<InntektService>()

        val aktiveArbeidsforholdService = ArbeidInntektYtelseService(arbeidsforholdService, inntektService)

        val arbeidsforholdAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals")
        val inntektAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(arbeidsforholdliste)

        every {
            inntektService.hentInntekter(aktørId, YearMonth.from(fom), YearMonth.from(tom))
        } returns Either.Right(inntekter)

        every {
            inntektService.hentFrilansarbeidsforhold(aktørId, YearMonth.from(fom), YearMonth.from(tom))
        } returns Either.Right(emptyList())

        val actual = aktiveArbeidsforholdService.finnArbeidsforholdMedInntekter(aktørId, fom, tom)

        val arbeidsforholdAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals")
        val inntektAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        assertEquals(0.0, arbeidsforholdAvviksCounterAfter - arbeidsforholdAvviksCounterBefore)
        assertEquals(0.0, inntektAvviksCounterAfter - inntektAvviksCounterBefore)

        when (actual) {
            is Either.Right -> {
                assertEquals(2, actual.right.arbeidsforhold.size)
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
                Arbeidsforhold(Arbeidsgiver.Virksomhet(Organisasjon.Virksomhet(Organisasjonsnummer("889640782"), "S. VINDEL & SØNN")), fom)
        )

        val inntekter = listOf(
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")), YearMonth.of(2019, 1), BigDecimal(20000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")), YearMonth.of(2019, 2), BigDecimal(25000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), YearMonth.of(2018, 10), BigDecimal(15000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), YearMonth.of(2018, 11), BigDecimal(16000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), YearMonth.of(2018, 12), BigDecimal(17000))
        )

        val virksomhet1 = Organisasjonsnummer("889640782")
        val expected = ArbeidInntektYtelse(
                arbeidsforhold = mapOf(
                        Arbeidsforhold(Arbeidsgiver.Virksomhet(Organisasjon.Virksomhet(virksomhet1, "S. VINDEL & SØNN")), fom) to listOf(
                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 1), BigDecimal(20000)),
                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 2), BigDecimal(25000))
                ))
        )

        val arbeidsforholdService = mockk<ArbeidsforholdService>()
        val inntektService = mockk<InntektService>()

        val aktiveArbeidsforholdService = ArbeidInntektYtelseService(arbeidsforholdService, inntektService)

        val arbeidsforholdAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals")
        val inntektAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(arbeidsforholdliste)

        every {
            inntektService.hentInntekter(aktørId, YearMonth.from(fom), YearMonth.from(tom))
        } returns Either.Right(inntekter)

        every {
            inntektService.hentFrilansarbeidsforhold(aktørId, YearMonth.from(fom), YearMonth.from(tom))
        } returns Either.Right(emptyList())

        val actual = aktiveArbeidsforholdService.finnArbeidsforholdMedInntekter(aktørId, fom, tom)

        val arbeidsforholdAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals")
        val inntektAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        assertEquals(0.0, arbeidsforholdAvviksCounterAfter - arbeidsforholdAvviksCounterBefore)
        assertEquals(3.0, inntektAvviksCounterAfter - inntektAvviksCounterBefore)

        when (actual) {
            is Either.Right -> {
                assertEquals(1, actual.right.arbeidsforhold.size)
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
                Arbeidsforhold(Arbeidsgiver.Virksomhet(Organisasjon.Virksomhet(Organisasjonsnummer("889640782"), "S. VINDEL & SØNN")), fom),
                Arbeidsforhold(Arbeidsgiver.Virksomhet(Organisasjon.Virksomhet(Organisasjonsnummer("995298775"), "Matbutikken A/S")), LocalDate.parse("2018-01-01"), fom)
        )

        val inntekter = listOf(
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), YearMonth.of(2018, 10), BigDecimal(15000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), YearMonth.of(2018, 11), BigDecimal(16000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), YearMonth.of(2018, 12), BigDecimal(17000))
        )

        val virksomhet2 = Organisasjonsnummer("995298775")
        val expected = ArbeidInntektYtelse(
                arbeidsforhold = mapOf(
                        Arbeidsforhold(Arbeidsgiver.Virksomhet(Organisasjon.Virksomhet(virksomhet2, "Matbutikken A/S")), LocalDate.parse("2018-01-01"), fom) to listOf(
                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 10), BigDecimal(15000)),
                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 11), BigDecimal(16000)),
                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 12), BigDecimal(17000))
                ))
        )

        val arbeidsforholdService = mockk<ArbeidsforholdService>()
        val inntektService = mockk<InntektService>()

        val aktiveArbeidsforholdService = ArbeidInntektYtelseService(arbeidsforholdService, inntektService)

        val arbeidsforholdAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals")
        val inntektAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(arbeidsforholdliste)

        every {
            inntektService.hentInntekter(aktørId, YearMonth.from(fom), YearMonth.from(tom))
        } returns Either.Right(inntekter)

        every {
            inntektService.hentFrilansarbeidsforhold(aktørId, YearMonth.from(fom), YearMonth.from(tom))
        } returns Either.Right(emptyList())

        val actual = aktiveArbeidsforholdService.finnArbeidsforholdMedInntekter(aktørId, fom, tom)

        val arbeidsforholdAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals")
        val inntektAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        assertEquals(1.0, arbeidsforholdAvviksCounterAfter - arbeidsforholdAvviksCounterBefore)
        assertEquals(0.0, inntektAvviksCounterAfter - inntektAvviksCounterBefore)

        when (actual) {
            is Either.Right -> {
                assertEquals(1, actual.right.arbeidsforhold.size)
                assertEquals(expected, actual.right)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }
}
