package no.nav.helse.probe

import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.helse.domene.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.domene.arbeid.domain.Arbeidsavtale
import no.nav.helse.domene.arbeid.domain.Arbeidsforhold
import no.nav.helse.domene.organisasjon.domain.Organisasjonsnummer
import no.nav.helse.domene.utbetaling.domain.UtbetalingEllerTrekk
import no.nav.helse.domene.utbetaling.domain.Virksomhet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

class DatakvalitetProbeTest {

    private val datakvalitetProbe = DatakvalitetProbe(mockk(relaxed = true), mockk())

    @Test
    fun `skal telle arbeidsforhold i samme virksomhet`() {
        val given = ArbeidInntektYtelse(
                arbeidsforhold = listOf(
                        aktivt_arbeidstakerforhold,
                        aktivt_arbeidstakerforhold_i_samme_virksomhet
                )
        )

        val arbeidsforholdISammeVirksomhetCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_i_samme_virksomhet_totals") ?: 0.0

        datakvalitetProbe.inspiserArbeidInntektYtelse(given)

        val arbeidsforholdISammeVirksomhetCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_i_samme_virksomhet_totals") ?: 0.0

        assertEquals(1.0, arbeidsforholdISammeVirksomhetCounterAfter - arbeidsforholdISammeVirksomhetCounterBefore)
    }

    @Test
    fun `skal telle inntekter som ikke har et tilhørende arbeidsforhold`() {
        val given = ArbeidInntektYtelse(
                arbeidsforhold = listOf(),
                lønnsinntekter = listOf(
                        lønn_virksomhet1_januar to emptyList(),
                        lønn_virksomhet1_februar to emptyList()
                )
        )

        val inntektAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        datakvalitetProbe.inspiserArbeidInntektYtelse(given)

        val inntektAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        assertEquals(2.0, inntektAvviksCounterAfter - inntektAvviksCounterBefore)
    }

    @Test
    fun `skal telle arbeidsforhold som ikke har tilhørende inntekter`() {
        val given = ArbeidInntektYtelse(
                arbeidsforhold = listOf(
                        aktivt_arbeidstakerforhold,
                        avsluttet_arbeidstakerforhold
                ),
                lønnsinntekter = listOf(
                        lønn_virksomhet1_januar to listOf(aktivt_arbeidstakerforhold),
                        lønn_virksomhet1_februar to listOf(aktivt_arbeidstakerforhold)
                )
        )

        val arbeidsforholdAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals", arrayOf("type"), arrayOf("Arbeidstaker")) ?: 0.0

        datakvalitetProbe.inspiserArbeidInntektYtelse(given)

        val arbeidsforholdAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals", arrayOf("type"), arrayOf("Arbeidstaker")) ?: 0.0


        assertEquals(1.0, arbeidsforholdAvviksCounterAfter - arbeidsforholdAvviksCounterBefore)
    }
}

private val orgnr1 = Organisasjonsnummer("995298775")
private val orgnr2 = Organisasjonsnummer("889640782")

private val virksomhet1 = Virksomhet.Organisasjon(orgnr1)
private val virksomhet2 = Virksomhet.Organisasjon(orgnr2)

private val arbeidsforholdId1 = 1234L
private val arbeidsforholdId2 = 5678L
private val arbeidsforholdId3 = 4321L

private val aktivt_arbeidstakerforhold_startdato = LocalDate.parse("2019-01-01")
private val
        aktivt_arbeidstakerforhold = Arbeidsforhold.Arbeidstaker(
        arbeidsgiver = virksomhet1,
        startdato = aktivt_arbeidstakerforhold_startdato,
        arbeidsforholdId = arbeidsforholdId1,
        arbeidsavtaler = listOf(
                Arbeidsavtale("Butikkmedarbeider", BigDecimal(100), aktivt_arbeidstakerforhold_startdato, null)
        ))

private val aktivt_arbeidstakerforhold_i_samme_virksomhet_startdato = LocalDate.parse("2018-12-01")
private val aktivt_arbeidstakerforhold_i_samme_virksomhet = Arbeidsforhold.Arbeidstaker(
        arbeidsgiver = virksomhet1,
        startdato = aktivt_arbeidstakerforhold_i_samme_virksomhet_startdato,
        arbeidsforholdId = arbeidsforholdId2,
        arbeidsavtaler = listOf(
                Arbeidsavtale("Butikkmedarbeider", BigDecimal(100), aktivt_arbeidstakerforhold_i_samme_virksomhet_startdato, null)
        ))

private val avsluttet_arbeidstakerforhold_startdato = LocalDate.parse("2018-01-01")
private val avsluttet_arbeidstakerforhold_sluttdato = LocalDate.parse("2018-12-31")
private val avsluttet_arbeidstakerforhold = Arbeidsforhold.Arbeidstaker(
        arbeidsgiver = virksomhet2,
        startdato = avsluttet_arbeidstakerforhold_startdato,
        sluttdato = avsluttet_arbeidstakerforhold_sluttdato,
        arbeidsforholdId = arbeidsforholdId3,
        arbeidsavtaler = listOf(
                Arbeidsavtale("Butikkmedarbeider", BigDecimal(100), avsluttet_arbeidstakerforhold_startdato, avsluttet_arbeidstakerforhold_sluttdato)
        ))

private val januar_2019 = YearMonth.of(2019, Month.JANUARY)
private val februar_2019 = YearMonth.of(2019, Month.FEBRUARY)

private val lønn_virksomhet1_januar = UtbetalingEllerTrekk.Lønn(virksomhet1, januar_2019, BigDecimal(20000))
private val lønn_virksomhet1_februar = UtbetalingEllerTrekk.Lønn(virksomhet1, februar_2019, BigDecimal(25000))
