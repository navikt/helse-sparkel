package no.nav.helse.domene.sykepengegrunnlag

import arrow.core.Either
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.utbetaling.UtbetalingOgTrekkService
import no.nav.helse.domene.utbetaling.domain.UtbetalingEllerTrekk.Lønn
import no.nav.helse.domene.utbetaling.domain.Virksomhet
import no.nav.helse.domene.organisasjon.OrganisasjonService
import no.nav.helse.domene.organisasjon.domain.InngårIJuridiskEnhet
import no.nav.helse.domene.organisasjon.domain.Organisasjon
import no.nav.helse.domene.organisasjon.domain.Organisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class SykepengegrunnlagServiceTest {

    @Test
    fun `skal gi feil når oppslag for organisasjon gir feil`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val virksomhetsnummer = Organisasjonsnummer("995298775")

        val organisasjonService = mockk<OrganisasjonService>()
        every {
            organisasjonService.hentOrganisasjon(virksomhetsnummer)
        } returns Either.Left(Feilårsak.FeilFraTjeneste)

        val inntektService = mockk<UtbetalingOgTrekkService>()

        val actual = SykepengegrunnlagService(inntektService, organisasjonService)
                .hentBeregningsgrunnlag(aktør, virksomhetsnummer, fom, tom)

        when (actual) {
            is Either.Left -> assertTrue(actual.a is Feilårsak.FeilFraTjeneste)
            else -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal gi feil når virksomhetsnummer ikke er en virksomhet`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val virksomhetsnummer = Organisasjonsnummer("995298775")

        val organisasjonService = mockk<OrganisasjonService>()
        every {
            organisasjonService.hentOrganisasjon(virksomhetsnummer)
        } returns Either.Right(Organisasjon.JuridiskEnhet(virksomhetsnummer))

        val inntektService = mockk<UtbetalingOgTrekkService>()

        val actual = SykepengegrunnlagService(inntektService, organisasjonService)
                .hentBeregningsgrunnlag(aktør, virksomhetsnummer, fom, tom)

        when (actual) {
            is Either.Left -> assertTrue(actual.a is Feilårsak.FeilFraBruker)
            else -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal gi inntekter på virksomhetsnummer og juridisk nummer`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val virksomhetsnummer = Organisasjonsnummer("995298775")
        val juridiskNummer = Organisasjonsnummer("889640782")
        val etAnnetOrganisasjonsnummer = Organisasjonsnummer("971524960")

        val expected = listOf(
                Lønn(Virksomhet.Organisasjon(virksomhetsnummer), fom, BigDecimal.valueOf(2500)),
                Lønn(Virksomhet.Organisasjon(juridiskNummer), fom, BigDecimal.valueOf(500))
        )

        val organisasjonService = mockk<OrganisasjonService>()
        every {
            organisasjonService.hentOrganisasjon(virksomhetsnummer)
        } returns Either.Right(Organisasjon.Virksomhet(virksomhetsnummer, null, listOf(InngårIJuridiskEnhet(juridiskNummer, LocalDate.now(), null))))

        val inntektService = mockk<UtbetalingOgTrekkService>()
        every {
            inntektService.hentUtbetalingerOgTrekk(aktør, fom, tom, "8-28")
        } returns Either.Right(listOf(
                Lønn(Virksomhet.Organisasjon(virksomhetsnummer), fom, BigDecimal.valueOf(2500)),
                Lønn(Virksomhet.Organisasjon(juridiskNummer), fom, BigDecimal.valueOf(500)),
                Lønn(Virksomhet.Organisasjon(etAnnetOrganisasjonsnummer), fom, BigDecimal.valueOf(1500))))

        val actual = SykepengegrunnlagService(inntektService, organisasjonService)
                .hentBeregningsgrunnlag(aktør, virksomhetsnummer, fom, tom)

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

        val inntektService = mockk<UtbetalingOgTrekkService>()
        every {
            inntektService.hentUtbetalingerOgTrekk(aktør, fom, tom, "8-30")
        } returns Either.Left(Feilårsak.FeilFraTjeneste)

        val actual = SykepengegrunnlagService(inntektService, mockk())
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
                Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(2500))
        )

        val inntektService = mockk<UtbetalingOgTrekkService>()
        every {
            inntektService.hentUtbetalingerOgTrekk(aktør, fom, tom, "8-30")
        } returns Either.Right(listOf(
                Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(2500))
        ))

        val actual = SykepengegrunnlagService(inntektService, mockk())
                .hentSammenligningsgrunnlag(aktør, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(expected, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }
}
