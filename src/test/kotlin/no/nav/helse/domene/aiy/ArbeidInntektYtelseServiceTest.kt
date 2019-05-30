package no.nav.helse.domene.aiy

import arrow.core.Either
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.aiy.domain.*
import no.nav.helse.domene.aiy.inntektskomponenten.UtbetalingOgTrekkService
import no.nav.helse.domene.aiy.organisasjon.OrganisasjonService
import no.nav.helse.domene.aiy.organisasjon.domain.DriverVirksomhet
import no.nav.helse.domene.aiy.organisasjon.domain.InngårIJuridiskEnhet
import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon
import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjonsnummer
import no.nav.helse.probe.DatakvalitetProbe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month.*
import java.time.YearMonth

class ArbeidInntektYtelseServiceTest {

    lateinit var arbeidsforholdService: ArbeidsforholdService
    lateinit var utbetalingOgTrekkService: UtbetalingOgTrekkService
    lateinit var organisasjonService: OrganisasjonService

    lateinit var aktiveArbeidsforholdService: ArbeidInntektYtelseService

    val aktørId = AktørId("123456789")
    val fom = LocalDate.parse("2019-01-01")
    val tom = LocalDate.parse("2019-03-01")

    @BeforeEach
    fun `initialize mocks and counters`() {
        arbeidsforholdService = mockk()
        utbetalingOgTrekkService = mockk()
        organisasjonService = mockk()

        aktiveArbeidsforholdService = ArbeidInntektYtelseService(arbeidsforholdService, utbetalingOgTrekkService, organisasjonService, DatakvalitetProbe(mockk(relaxed = true), mockk()))
    }

    @Test
    fun `inntekt på virksomhetsnummer skal matche arbeidsforhold på samme virksomhetsnummer`() {
        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(listOf(
                aktivt_arbeidstakerforhold
        ))

        every {
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, YearMonth.from(fom), YearMonth.from(tom), "ForeldrepengerA-Inntekt")
        } returns Either.Right(listOf(
                lønn_virksomhet1_januar,
                lønn_virksomhet1_februar
        ))

        every {
            organisasjonService.hentOrganisasjon(virksomhet1.organisasjonsnummer)
        } returns Either.Right(Organisasjon.Virksomhet(virksomhet1.organisasjonsnummer))

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(forventet_resultat_inntekter_på_virksomhetsnummer, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `inntekt på personnummer skal matche arbeidsforhold på samme personnummer`() {
        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(listOf(
                aktivt_frilansforhold_privat,
                aktivt_arbeidstakerforhold_privat
        ))

        every {
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, YearMonth.from(fom), YearMonth.from(tom), "ForeldrepengerA-Inntekt")
        } returns Either.Right(listOf(
                lønn_person1_januar,
                lønn_person1_februar,
                lønn_person2_februar
        ))

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(forventet_resultat_inntekter_på_personnummer, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `inntekt på personnummer og juridisk nummer skal matche arbeidsforhold rapportert på samme nummer`() {
        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(listOf(
                aktivt_arbeidstakerforhold,
                aktivt_arbeidstakerforhold_privat
        ))

        every {
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, YearMonth.from(fom), YearMonth.from(tom), "ForeldrepengerA-Inntekt")
        } returns Either.Right(listOf(
                lønn_virksomhet2_oktober,
                lønn_virksomhet2_november,
                lønn_person2_februar
        ))

        every {
            organisasjonService.hentOrganisasjon(virksomhet2.organisasjonsnummer)
        } returns Either.Right(Organisasjon.JuridiskEnhet(virksomhet2.organisasjonsnummer, null, listOf(
                DriverVirksomhet(Organisasjon.Virksomhet(virksomhet1.organisasjonsnummer), LocalDate.now(), null)
        )))

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(forventet_resultat_inntekter_på_personnummer_og_orgnummer, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `inntekt på virksomhetsnummer skal matche flere arbeidsforhold på samme virksomhetsnummer`() {
        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(listOf(
                aktivt_arbeidstakerforhold,
                aktivt_arbeidstakerforhold_i_samme_virksomhet
        ))

        every {
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, YearMonth.from(fom), YearMonth.from(tom), "ForeldrepengerA-Inntekt")
        } returns Either.Right(listOf(
                lønn_virksomhet1_januar,
                lønn_virksomhet1_februar
        ))

        every {
            organisasjonService.hentOrganisasjon(virksomhet1.organisasjonsnummer)
        } returns Either.Right(Organisasjon.Virksomhet(virksomhet1.organisasjonsnummer))

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(forventet_resultat_inntekter_på_virksomhetsnummer_med_flere_arbeidsforhold_i_samme, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `inntekt på virksomhetsnummer skal matche arbeidsforhold på samme virksomhetsnummer og frilans arbeidsforhold på juridisk nummer`() {
        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(listOf(
                aktivt_arbeidstakerforhold,
                aktivt_frilansforhold
        ))

        every {
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, YearMonth.from(fom), YearMonth.from(tom), "ForeldrepengerA-Inntekt")
        } returns Either.Right(listOf(
                lønn_virksomhet1_januar,
                lønn_virksomhet1_februar
        ))

        every {
            organisasjonService.hentOrganisasjon(virksomhet1.organisasjonsnummer)
        } returns Either.Right(Organisasjon.Virksomhet(virksomhet1.organisasjonsnummer, null, listOf(
                InngårIJuridiskEnhet(virksomhet3.organisasjonsnummer, LocalDate.now(), null)
        )))

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(forventet_resultat_inntekter_på_virksomhetsnummer_med_frilans_på_juridisk_nummer, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `inntekt på juridisk nummer skal matche arbeidsforhold på virksomhetsnummer`() {
        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(listOf(
                aktivt_arbeidstakerforhold
        ))

        every {
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, YearMonth.from(fom), YearMonth.from(tom), "ForeldrepengerA-Inntekt")
        } returns Either.Right(listOf(
                lønn_virksomhet2_oktober,
                lønn_virksomhet2_november,
                lønn_virksomhet2_desember
        ))

        every {
            organisasjonService.hentOrganisasjon(virksomhet2.organisasjonsnummer)
        } returns Either.Right(Organisasjon.JuridiskEnhet(virksomhet2.organisasjonsnummer, null, listOf(
                DriverVirksomhet(Organisasjon.Virksomhet(virksomhet1.organisasjonsnummer), LocalDate.now(), null)
        )))

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(forventet_resultat_inntekter_på_juridisk_nummer, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `inntekt på gammelt juridisk nummer skal matche arbeidsforhold på virksomhetsnummer med nytt juridisk nummer`() {
        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(listOf(
                aktivt_arbeidstakerforhold
        ))

        every {
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, YearMonth.from(fom), YearMonth.from(tom), "ForeldrepengerA-Inntekt")
        } returns Either.Right(listOf(
                lønn_virksomhet2_oktober,
                lønn_virksomhet2_november,
                lønn_virksomhet2_desember
        ))

        every {
            organisasjonService.hentOrganisasjon(virksomhet2.organisasjonsnummer)
        } returns Either.Right(Organisasjon.JuridiskEnhet(virksomhet2.organisasjonsnummer, null, listOf(
                DriverVirksomhet(Organisasjon.Virksomhet(virksomhet1.organisasjonsnummer), LocalDate.now().minusYears(2), LocalDate.now().minusYears(1))
        )))

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(forventet_resultat_inntekter_på_juridisk_nummer, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `inntekt på juridisk nummer skal matche arbeidsforhold på virksomhetsnummer og frilans arbeidsforhold på juridisk nummer`() {
        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(listOf(
                aktivt_arbeidstakerforhold,
                aktivt_frilansforhold
        ))

        every {
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, YearMonth.from(fom), YearMonth.from(tom), "ForeldrepengerA-Inntekt")
        } returns Either.Right(listOf(
                lønn_virksomhet3_desember,
                lønn_virksomhet3_februar
        ))

        every {
            organisasjonService.hentOrganisasjon(virksomhet3.organisasjonsnummer)
        } returns Either.Right(Organisasjon.JuridiskEnhet(virksomhet3.organisasjonsnummer, null, listOf(
                DriverVirksomhet(Organisasjon.Virksomhet(virksomhet1.organisasjonsnummer), LocalDate.now(), null)
        )))

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(forventet_resultat_inntekter_på_juridisk_nummer_med_arbeidstaker_og_frilans, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal sammenstille arbeidsforhold og inntekter`() {
        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(listOf(
                aktivt_arbeidstakerforhold,
                avsluttet_arbeidstakerforhold,
                aktivt_frilansforhold
        ))

        every {
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, YearMonth.from(fom), YearMonth.from(tom), "ForeldrepengerA-Inntekt")
        } returns Either.Right(inntekter_fra_tre_virksomheter)

        every {
            organisasjonService.hentOrganisasjon(virksomhet1.organisasjonsnummer)
        } returns Either.Right(Organisasjon.Virksomhet(virksomhet1.organisasjonsnummer))

        every {
            organisasjonService.hentOrganisasjon(virksomhet2.organisasjonsnummer)
        } returns Either.Right(Organisasjon.Virksomhet(virksomhet2.organisasjonsnummer))

        every {
            organisasjonService.hentOrganisasjon(virksomhet3.organisasjonsnummer)
        } returns Either.Right(Organisasjon.Virksomhet(virksomhet3.organisasjonsnummer))

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(forventet_resultat_uten_avvik, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal ikke slå sammen to arbeidsforhold under samme virksomhet`() {
        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(listOf(
                aktivt_arbeidstakerforhold,
                aktivt_arbeidstakerforhold_i_samme_virksomhet
        ))

        every {
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, YearMonth.from(fom), YearMonth.from(tom), "ForeldrepengerA-Inntekt")
        } returns Either.Right(inntekter_fra_samme_virksomhet_med_juridisk_nummer)

        every {
            organisasjonService.hentOrganisasjon(virksomhet1.organisasjonsnummer)
        } returns Either.Right(Organisasjon.Virksomhet(virksomhet1.organisasjonsnummer))

        every {
            organisasjonService.hentOrganisasjon(virksomhet2.organisasjonsnummer)
        } returns Either.Right(Organisasjon.JuridiskEnhet(virksomhet2.organisasjonsnummer, null, listOf(
                DriverVirksomhet(Organisasjon.Virksomhet(virksomhet1.organisasjonsnummer), LocalDate.now(), null)
        )))

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(forventet_resultat_med_flere_arbeidsforhold_i_samme_virksomhet, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal gruppere ytelser og trygd`() {
        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(emptyList())

        every {
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, YearMonth.from(fom), YearMonth.from(tom), "ForeldrepengerA-Inntekt")
        } returns Either.Right(inntekter_med_ytelser_og_trygd)

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(forventet_resultat_med_ytelser_og_trygd, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal sammenstille inntekter rapportert på jurdisk nummer med arbeidsforhold rapporter på virksomhetsnummer`() {
        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(listOf(
                aktivt_arbeidstakerforhold,
                aktivt_arbeidstakerforhold_i_annen_virksomhet
        ))

        every {
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, YearMonth.from(fom), YearMonth.from(tom), "ForeldrepengerA-Inntekt")
        } returns Either.Right(listOf(
                lønn_virksomhet1_oktober,
                lønn_virksomhet2_november,
                lønn_virksomhet2_desember,
                lønn_virksomhet4_desember
        ))

        every {
            organisasjonService.hentOrganisasjon(virksomhet1.organisasjonsnummer)
        } returns Either.Right(Organisasjon.Virksomhet(virksomhet1.organisasjonsnummer))

        every {
            organisasjonService.hentOrganisasjon(virksomhet2.organisasjonsnummer)
        } returns Either.Right(Organisasjon.JuridiskEnhet(virksomhet2.organisasjonsnummer, "ARBEIDS- OG VELFERDSETATEN", listOf(
                DriverVirksomhet(Organisasjon.Virksomhet(virksomhet1.organisasjonsnummer), LocalDate.now(), null)
        )))

        every {
            organisasjonService.hentOrganisasjon(virksomhet4.organisasjonsnummer)
        } returns Either.Right(Organisasjon.JuridiskEnhet(virksomhet4.organisasjonsnummer, "STORTINGET", listOf(
                DriverVirksomhet(Organisasjon.Virksomhet(virksomhet3.organisasjonsnummer), LocalDate.now(), null)
        )))

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(forventet_resultat_inntekter_slått_sammen_med_arbeidsforhold, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal gi feil når organisasjonoppslag for juridisk enhet ender i feil`() {
        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(listOf(
                aktivt_arbeidstakerforhold
        ))

        every {
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, YearMonth.from(fom), YearMonth.from(tom), "ForeldrepengerA-Inntekt")
        } returns Either.Right(listOf(
                lønn_virksomhet2_oktober,
                lønn_virksomhet2_november,
                lønn_virksomhet2_desember
        ))

        every {
            organisasjonService.hentOrganisasjon(virksomhet2.organisasjonsnummer)
        } returns Either.Left(Feilårsak.FeilFraTjeneste)

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.FeilFraTjeneste, actual.a)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
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
                Arbeidsavtale.Gjeldende("Butikkmedarbeider", BigDecimal(100), aktivt_arbeidstakerforhold_startdato)
        ))

private val aktivt_arbeidstakerforhold_i_samme_virksomhet_startdato = LocalDate.parse("2018-12-01")
private val aktivt_arbeidstakerforhold_i_samme_virksomhet = Arbeidsforhold.Arbeidstaker(
        arbeidsgiver = virksomhet1,
        startdato = aktivt_arbeidstakerforhold_i_samme_virksomhet_startdato,
        arbeidsforholdId = arbeidsforholdId2,
        arbeidsavtaler = listOf(
                Arbeidsavtale.Gjeldende("Butikkmedarbeider", BigDecimal(100), aktivt_arbeidstakerforhold_i_samme_virksomhet_startdato)
        ))

private val aktivt_arbeidstakerforhold_i_annen_virksomhet_startdato = LocalDate.parse("2019-01-01")
private val aktivt_arbeidstakerforhold_i_annen_virksomhet = Arbeidsforhold.Arbeidstaker(
        arbeidsgiver = virksomhet3,
        startdato = aktivt_arbeidstakerforhold_i_annen_virksomhet_startdato,
        arbeidsforholdId = arbeidsforholdId2,
        arbeidsavtaler = listOf(
                Arbeidsavtale.Gjeldende("Butikkmedarbeider", BigDecimal(100), aktivt_arbeidstakerforhold_i_annen_virksomhet_startdato)
        ))

private val avsluttet_arbeidstakerforhold_startdato = LocalDate.parse("2018-01-01")
private val avsluttet_arbeidstakerforhold_sluttdato = LocalDate.parse("2018-12-31")
private val avsluttet_arbeidstakerforhold = Arbeidsforhold.Arbeidstaker(
        arbeidsgiver = virksomhet2,
        startdato = avsluttet_arbeidstakerforhold_startdato,
        sluttdato = avsluttet_arbeidstakerforhold_sluttdato,
        arbeidsforholdId = arbeidsforholdId3,
        arbeidsavtaler = listOf(
                Arbeidsavtale.Gjeldende("Butikkmedarbeider", BigDecimal(100), avsluttet_arbeidstakerforhold_startdato)
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
                Arbeidsavtale.Gjeldende("ALTMULIGMANN", null, LocalDate.now())
        )
)

private val januar_2019 = YearMonth.of(2019, JANUARY)
private val februar_2019 = YearMonth.of(2019, FEBRUARY)
private val mars_2019 = YearMonth.of(2019, MARCH)
private val oktober_2018 = YearMonth.of(2018, OCTOBER)
private val november_2018 = YearMonth.of(2018, NOVEMBER)
private val desember_2018 = YearMonth.of(2018, DECEMBER)

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

private val forventet_resultat_inntekter_på_personnummer_og_orgnummer = ArbeidInntektYtelse(
        arbeidsforhold = listOf(
                aktivt_arbeidstakerforhold,
                aktivt_arbeidstakerforhold_privat
        ),
        lønnsinntekter = listOf(
                lønn_virksomhet2_oktober to listOf(aktivt_arbeidstakerforhold),
                lønn_virksomhet2_november to listOf(aktivt_arbeidstakerforhold),
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
