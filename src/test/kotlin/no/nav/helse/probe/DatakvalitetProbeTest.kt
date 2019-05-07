package no.nav.helse.probe

import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.helse.domene.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.domene.arbeid.domain.Arbeidsavtale
import no.nav.helse.domene.arbeid.domain.Arbeidsforhold
import no.nav.helse.domene.utbetaling.domain.UtbetalingEllerTrekk
import no.nav.helse.domene.utbetaling.domain.Virksomhet
import no.nav.helse.domene.organisasjon.domain.Organisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

class DatakvalitetProbeTest {

    private val datakvalitetProbe = DatakvalitetProbe(mockk(relaxed = true))

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
private val orgnr3 = Organisasjonsnummer("971524960")
private val orgnr4 = Organisasjonsnummer("912998827")

private val virksomhet1 = Virksomhet.Organisasjon(orgnr1)
private val virksomhet2 = Virksomhet.Organisasjon(orgnr2)
private val virksomhet3 = Virksomhet.Organisasjon(orgnr3)
private val virksomhet4 = Virksomhet.Organisasjon(orgnr4)

private val person1 = Virksomhet.Person("12345678911")
private val person2 = Virksomhet.Person("98765432199")

private val arbeidsforholdId1 = 1234L
private val arbeidsforholdId2 = 5678L
private val arbeidsforholdId3 = 4321L
private val arbeidsforholdId4 = 1122L

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

private val aktivt_arbeidstakerforhold_i_annen_virksomhet_startdato = LocalDate.parse("2019-01-01")
private val aktivt_arbeidstakerforhold_i_annen_virksomhet = Arbeidsforhold.Arbeidstaker(
        arbeidsgiver = virksomhet3,
        startdato = aktivt_arbeidstakerforhold_i_annen_virksomhet_startdato,
        arbeidsforholdId = arbeidsforholdId2,
        arbeidsavtaler = listOf(
                Arbeidsavtale("Butikkmedarbeider", BigDecimal(100), aktivt_arbeidstakerforhold_i_annen_virksomhet_startdato, null)
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

private val aktivt_frilansforhold_startdato = LocalDate.parse("2019-01-01")
private val aktivt_frilansforhold = Arbeidsforhold.Frilans(
        arbeidsgiver = virksomhet3,
        startdato = aktivt_frilansforhold_startdato,
        sluttdato = null,
        yrke = "Butikkmedarbeider")

private val aktivt_frilansforhold_privat = Arbeidsforhold.Frilans(
        arbeidsgiver = person1,
        startdato = LocalDate.now(),
        sluttdato = null,
        yrke = ""
)

private val aktivt_arbeidstakerforhold_privat = Arbeidsforhold.Arbeidstaker(
        arbeidsgiver = person2,
        startdato = LocalDate.now(),
        sluttdato = null,
        arbeidsforholdId = arbeidsforholdId4,
        permisjon = emptyList(),
        arbeidsavtaler = listOf(
                Arbeidsavtale("ALTMULIGMANN", null, LocalDate.now(), null)
        )
)

private val januar_2019 = YearMonth.of(2019, Month.JANUARY)
private val februar_2019 = YearMonth.of(2019, Month.FEBRUARY)
private val mars_2019 = YearMonth.of(2019, Month.MARCH)
private val oktober_2018 = YearMonth.of(2018, Month.OCTOBER)
private val november_2018 = YearMonth.of(2018, Month.NOVEMBER)
private val desember_2018 = YearMonth.of(2018, Month.DECEMBER)

private val lønn_virksomhet1_januar = UtbetalingEllerTrekk.Lønn(virksomhet1, januar_2019, BigDecimal(20000))
private val lønn_virksomhet1_februar = UtbetalingEllerTrekk.Lønn(virksomhet1, februar_2019, BigDecimal(25000))
private val lønn_virksomhet1_oktober = UtbetalingEllerTrekk.Lønn(virksomhet1, oktober_2018, BigDecimal(15000))
private val lønn_virksomhet2_oktober = UtbetalingEllerTrekk.Lønn(virksomhet2, oktober_2018, BigDecimal(15000))
private val lønn_virksomhet2_november = UtbetalingEllerTrekk.Lønn(virksomhet2, november_2018, BigDecimal(16000))
private val lønn_virksomhet2_desember = UtbetalingEllerTrekk.Lønn(virksomhet2, desember_2018, BigDecimal(17000))
private val lønn_virksomhet3_desember = UtbetalingEllerTrekk.Lønn(virksomhet3, desember_2018, BigDecimal(18000))
private val lønn_virksomhet4_desember = UtbetalingEllerTrekk.Lønn(virksomhet4, desember_2018, BigDecimal(18000))
private val lønn_virksomhet3_februar = UtbetalingEllerTrekk.Lønn(virksomhet3, februar_2019, BigDecimal(30000))

private val lønn_person1_januar = UtbetalingEllerTrekk.Lønn(person1, januar_2019, BigDecimal(1000))
private val lønn_person1_februar = UtbetalingEllerTrekk.Lønn(person1, februar_2019, BigDecimal(500))
private val lønn_person2_februar = UtbetalingEllerTrekk.Lønn(person2, februar_2019, BigDecimal(1500))

private val inntekter_fra_tre_virksomheter = listOf(
        lønn_virksomhet1_januar,
        lønn_virksomhet1_februar,
        lønn_virksomhet2_oktober,
        lønn_virksomhet2_november,
        lønn_virksomhet2_desember,
        lønn_virksomhet3_februar
)

private val lønn_virksomhet1_mars = UtbetalingEllerTrekk.Lønn(virksomhet1, mars_2019, BigDecimal(30000))
private val lønn_virksomhet2_mars = UtbetalingEllerTrekk.Lønn(virksomhet2, mars_2019, BigDecimal(30000))

private val inntekter_fra_samme_virksomhet_med_juridisk_nummer = listOf(
        lønn_virksomhet1_januar,
        lønn_virksomhet1_februar,
        lønn_virksomhet2_mars
)

private val inntekter_med_ytelser_og_trygd = listOf(
        UtbetalingEllerTrekk.Ytelse(virksomhet1, YearMonth.of(2019, 1), BigDecimal(20000), "foreldrepenger"),
        UtbetalingEllerTrekk.Ytelse(virksomhet1, YearMonth.of(2019, 2), BigDecimal(25000), "sykepenger"),
        UtbetalingEllerTrekk.PensjonEllerTrygd(virksomhet2, YearMonth.of(2018, 10), BigDecimal(15000), "ufoerepensjonFraAndreEnnFolketrygden"),
        UtbetalingEllerTrekk.PensjonEllerTrygd(virksomhet2, YearMonth.of(2018, 11), BigDecimal(16000), "ufoerepensjonFraAndreEnnFolketrygden")
)

private val forventet_resultat_inntekter_på_personnummer = ArbeidInntektYtelse(
        arbeidsforhold = listOf(
                aktivt_frilansforhold_privat,
                aktivt_arbeidstakerforhold_privat
        ),
        lønnsinntekter = listOf(
                lønn_person1_januar to listOf(aktivt_frilansforhold_privat),
                lønn_person1_februar to listOf(aktivt_frilansforhold_privat),
                lønn_person2_februar to listOf(aktivt_arbeidstakerforhold_privat)
        ),
        ytelser = emptyList(),
        pensjonEllerTrygd = emptyList(),
        næringsinntekt = emptyList()
)

private val forventet_resultat_inntekter_på_virksomhetsnummer = ArbeidInntektYtelse(
        arbeidsforhold = listOf(
                aktivt_arbeidstakerforhold
        ),
        lønnsinntekter = listOf(
                lønn_virksomhet1_januar to listOf(aktivt_arbeidstakerforhold),
                lønn_virksomhet1_februar to listOf(aktivt_arbeidstakerforhold)
        ),
        ytelser = emptyList(),
        pensjonEllerTrygd = emptyList(),
        næringsinntekt = emptyList()
)

private val forventet_resultat_inntekter_på_virksomhetsnummer_med_frilans_på_juridisk_nummer = ArbeidInntektYtelse(
        arbeidsforhold = listOf(
                aktivt_arbeidstakerforhold,
                aktivt_frilansforhold
        ),
        lønnsinntekter = listOf(
                lønn_virksomhet1_januar to listOf(aktivt_arbeidstakerforhold, aktivt_frilansforhold),
                lønn_virksomhet1_februar to listOf(aktivt_arbeidstakerforhold, aktivt_frilansforhold)
        ),
        ytelser = emptyList(),
        pensjonEllerTrygd = emptyList(),
        næringsinntekt = emptyList()
)

private val forventet_resultat_inntekter_på_virksomhetsnummer_med_flere_arbeidsforhold_i_samme = ArbeidInntektYtelse(
        arbeidsforhold = listOf(
                aktivt_arbeidstakerforhold,
                aktivt_arbeidstakerforhold_i_samme_virksomhet
        ),
        lønnsinntekter = listOf(
                lønn_virksomhet1_januar to listOf(aktivt_arbeidstakerforhold, aktivt_arbeidstakerforhold_i_samme_virksomhet),
                lønn_virksomhet1_februar to listOf(aktivt_arbeidstakerforhold, aktivt_arbeidstakerforhold_i_samme_virksomhet)
        ),
        ytelser = emptyList(),
        pensjonEllerTrygd = emptyList(),
        næringsinntekt = emptyList()
)

private val forventet_resultat_inntekter_på_juridisk_nummer = ArbeidInntektYtelse(
        arbeidsforhold = listOf(
                aktivt_arbeidstakerforhold
        ),
        lønnsinntekter = listOf(
                lønn_virksomhet2_oktober to listOf(aktivt_arbeidstakerforhold),
                lønn_virksomhet2_november to listOf(aktivt_arbeidstakerforhold),
                lønn_virksomhet2_desember to listOf(aktivt_arbeidstakerforhold)
        ),
        ytelser = emptyList(),
        pensjonEllerTrygd = emptyList(),
        næringsinntekt = emptyList()
)

private val forventet_resultat_inntekter_på_juridisk_nummer_med_arbeidstaker_og_frilans = ArbeidInntektYtelse(
        arbeidsforhold = listOf(
                aktivt_arbeidstakerforhold,
                aktivt_frilansforhold
        ),
        lønnsinntekter = listOf(
                lønn_virksomhet3_desember to listOf(aktivt_arbeidstakerforhold, aktivt_frilansforhold),
                lønn_virksomhet3_februar to listOf(aktivt_arbeidstakerforhold, aktivt_frilansforhold)
        ),
        ytelser = emptyList(),
        pensjonEllerTrygd = emptyList(),
        næringsinntekt = emptyList()
)

private val forventet_resultat_uten_avvik = ArbeidInntektYtelse(
        arbeidsforhold = listOf(
                aktivt_arbeidstakerforhold,
                avsluttet_arbeidstakerforhold,
                aktivt_frilansforhold
        ),
        lønnsinntekter = listOf(
                lønn_virksomhet1_januar to listOf(aktivt_arbeidstakerforhold),
                lønn_virksomhet1_februar to listOf(aktivt_arbeidstakerforhold),
                lønn_virksomhet2_oktober to listOf(avsluttet_arbeidstakerforhold),
                lønn_virksomhet2_november to listOf(avsluttet_arbeidstakerforhold),
                lønn_virksomhet2_desember to listOf(avsluttet_arbeidstakerforhold),
                lønn_virksomhet3_februar to listOf(aktivt_frilansforhold)
        ),
        ytelser = emptyList(),
        pensjonEllerTrygd = emptyList(),
        næringsinntekt = emptyList()
)

private val forventet_resultat_med_flere_arbeidsforhold_i_samme_virksomhet = ArbeidInntektYtelse(
        arbeidsforhold = listOf(
                aktivt_arbeidstakerforhold,
                aktivt_arbeidstakerforhold_i_samme_virksomhet
        ),
        lønnsinntekter = listOf(
                lønn_virksomhet1_januar to listOf(aktivt_arbeidstakerforhold, aktivt_arbeidstakerforhold_i_samme_virksomhet),
                lønn_virksomhet1_februar to listOf(aktivt_arbeidstakerforhold, aktivt_arbeidstakerforhold_i_samme_virksomhet),
                lønn_virksomhet2_mars to listOf(aktivt_arbeidstakerforhold, aktivt_arbeidstakerforhold_i_samme_virksomhet)
        ),
        ytelser = emptyList(),
        pensjonEllerTrygd = emptyList(),
        næringsinntekt = emptyList()
)

private val forventet_resultat_med_ytelser_og_trygd = ArbeidInntektYtelse(
        ytelser = listOf(
                UtbetalingEllerTrekk.Ytelse(virksomhet1, YearMonth.of(2019, 1), BigDecimal(20000), "foreldrepenger"),
                UtbetalingEllerTrekk.Ytelse(virksomhet1, YearMonth.of(2019, 2), BigDecimal(25000), "sykepenger")
        ),
        pensjonEllerTrygd = listOf(
                UtbetalingEllerTrekk.PensjonEllerTrygd(virksomhet2, YearMonth.of(2018, 10), BigDecimal(15000), "ufoerepensjonFraAndreEnnFolketrygden"),
                UtbetalingEllerTrekk.PensjonEllerTrygd(virksomhet2, YearMonth.of(2018, 11), BigDecimal(16000), "ufoerepensjonFraAndreEnnFolketrygden")
        )
)

private val forventet_resultat_med_inntekter_uten_arbeidsforhold = ArbeidInntektYtelse(
        arbeidsforhold = listOf(
                aktivt_arbeidstakerforhold
        ),
        lønnsinntekter = listOf(
                lønn_virksomhet1_januar to listOf(aktivt_arbeidstakerforhold),
                lønn_virksomhet1_februar to listOf(aktivt_arbeidstakerforhold),
                lønn_virksomhet2_oktober to emptyList(),
                lønn_virksomhet2_november to emptyList(),
                lønn_virksomhet2_desember to emptyList(),
                lønn_virksomhet3_februar to emptyList()
        ),
        ytelser = emptyList(),
        pensjonEllerTrygd = emptyList(),
        næringsinntekt = emptyList()
)

private val forventet_resultat_med_arbeidsforhold_uten_inntekter = ArbeidInntektYtelse(
        arbeidsforhold = listOf(
                aktivt_arbeidstakerforhold,
                avsluttet_arbeidstakerforhold
        ),
        lønnsinntekter = listOf(
                lønn_virksomhet2_oktober to listOf(avsluttet_arbeidstakerforhold),
                lønn_virksomhet2_november to listOf(avsluttet_arbeidstakerforhold),
                lønn_virksomhet2_desember to listOf(avsluttet_arbeidstakerforhold)
        ),
        ytelser = emptyList(),
        pensjonEllerTrygd = emptyList(),
        næringsinntekt = emptyList()
)

private val forventet_resultat_inntekter_slått_sammen_med_arbeidsforhold = ArbeidInntektYtelse(
        arbeidsforhold = listOf(
                aktivt_arbeidstakerforhold,
                aktivt_arbeidstakerforhold_i_annen_virksomhet
        ),
        lønnsinntekter = listOf(
                lønn_virksomhet1_oktober to listOf(aktivt_arbeidstakerforhold),
                lønn_virksomhet2_november to listOf(aktivt_arbeidstakerforhold),
                lønn_virksomhet2_desember to listOf(aktivt_arbeidstakerforhold),
                lønn_virksomhet4_desember to listOf(aktivt_arbeidstakerforhold_i_annen_virksomhet)
        ),
        ytelser = emptyList(),
        pensjonEllerTrygd = emptyList(),
        næringsinntekt = emptyList()
)
