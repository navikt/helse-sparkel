package no.nav.helse.ws.sykepengegrunnlag

import arrow.core.Either
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Feilårsak
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.inntekt.InntektService
import no.nav.helse.ws.inntekt.domain.Virksomhet
import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.math.BigDecimal
import java.time.YearMonth

class SykepengegrunnlagServiceTest {

    @Test
    fun `skal gi feil når oppslag gir feil`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val inntektService = mockk<InntektService>()
        every {
            inntektService.hentInntekter(aktør, fom, tom, "8-28")
        } returns Either.Left(Feilårsak.FeilFraTjeneste)

        val actual = SykepengegrunnlagService(inntektService)
                .hentBeregningsgrunnlag(aktør, fom, tom)

        when (actual) {
            is Either.Left -> assertTrue(actual.a is Feilårsak.FeilFraTjeneste)
            else -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal returnere liste over inntekter`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val expected = listOf(
                no.nav.helse.ws.inntekt.domain.Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(2500)),
                no.nav.helse.ws.inntekt.domain.Inntekt.Ytelse(Virksomhet.Organisasjon(Organisasjonsnummer("995277670")),
                        fom, BigDecimal.valueOf(500), "barnetrygd"),
                no.nav.helse.ws.inntekt.domain.Inntekt.Næring(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(1500), "næringsinntekt"),
                no.nav.helse.ws.inntekt.domain.Inntekt.PensjonEllerTrygd(Virksomhet.Organisasjon(Organisasjonsnummer("995277670")),
                        fom, BigDecimal.valueOf(3000), "alderspensjon")
        )

        val inntektService = mockk<InntektService>()
        every {
            inntektService.hentInntekter(aktør, fom, tom, "8-28")
        } returns Either.Right(listOf(
                no.nav.helse.ws.inntekt.domain.Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(2500)),
                no.nav.helse.ws.inntekt.domain.Inntekt.Ytelse(Virksomhet.Organisasjon(Organisasjonsnummer("995277670")),
                        fom, BigDecimal.valueOf(500), "barnetrygd"),
                no.nav.helse.ws.inntekt.domain.Inntekt.Næring(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(1500), "næringsinntekt"),
                no.nav.helse.ws.inntekt.domain.Inntekt.PensjonEllerTrygd(Virksomhet.Organisasjon(Organisasjonsnummer("995277670")),
                        fom, BigDecimal.valueOf(3000), "alderspensjon")))

        val actual = SykepengegrunnlagService(inntektService)
                .hentBeregningsgrunnlag(aktør, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(expected, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }

    }

    @Test
    fun `skal gi feil når oppslag gir feil for sammenligningsgrunnlag`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val inntektService = mockk<InntektService>()
        every {
            inntektService.hentInntekter(aktør, fom, tom, "8-30")
        } returns Either.Left(Feilårsak.FeilFraTjeneste)

        val actual = SykepengegrunnlagService(inntektService)
                .hentSammenligningsgrunnlag(aktør, fom, tom)

        when (actual) {
            is Either.Left -> assertTrue(actual.a is Feilårsak.FeilFraTjeneste)
            else -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal returnere liste over inntekter for sammenligningsgrunnlag`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val expected = listOf(
                no.nav.helse.ws.inntekt.domain.Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(2500))
        )

        val inntektService = mockk<InntektService>()
        every {
            inntektService.hentInntekter(aktør, fom, tom, "8-30")
        } returns Either.Right(listOf(
                no.nav.helse.ws.inntekt.domain.Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(2500))
        ))

        val actual = SykepengegrunnlagService(inntektService)
                .hentSammenligningsgrunnlag(aktør, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(expected, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }
}
