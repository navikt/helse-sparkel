package no.nav.helse.domene.aiy.aareg

import arrow.core.Either
import arrow.core.Try
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Feilårsak
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.AktørId
import no.nav.helse.oppslag.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.domene.aiy.organisasjon.OrganisasjonService
import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.feil.Sikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.feil.UgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ArbeidsgiverServiceTest {

    @Test
    fun `skal returnere feil når arbeidsforholdoppslag gir feil`() {
        val arbeidsforholdClient = mockk<ArbeidsforholdClient>()
        val organisasjonService = mockk<OrganisasjonService>()

        val aktørId = AktørId("123456789")
        val fom = LocalDate.parse("2019-01-01")
        val tom = LocalDate.parse("2019-02-01")

        every {
            arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom)
        } returns Try.Failure(Exception("SOAP fault"))

        val actual = ArbeidsgiverService(arbeidsforholdClient, organisasjonService)
                .finnArbeidsgivere(aktørId, fom, tom)

        when (actual) {
            is Either.Left -> assertTrue(actual.a is Feilårsak.UkjentFeil)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal returnere en liste over organisasjoner`() {
        val arbeidsforholdClient = mockk<ArbeidsforholdClient>()
        val organisasjonService = mockk<OrganisasjonService>()

        val aktørId = AktørId("123456789")
        val fom = LocalDate.parse("2019-01-01")
        val tom = LocalDate.parse("2019-02-01")

        val expected = listOf(
                no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer(arbeidsgiver_organisasjon_1.orgnummer),
                        arbeidsgiver_organisasjon_1.navn),
                no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer(arbeidsgiver_organisasjon_2.orgnummer),
                        arbeidsgiver_organisasjon_2.navn)
        )

        every {
            arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom)
        } returns Try.Success(listOf(arbeidsforhold_uten_sluttdato, avsluttet_arbeidsforhold_med_permittering))

        val actual = ArbeidsgiverService(arbeidsforholdClient, organisasjonService)
                .finnArbeidsgivere(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(expected, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal slå opp navn på organisasjon`() {
        val arbeidsforholdClient = mockk<ArbeidsforholdClient>()
        val organisasjonService = mockk<OrganisasjonService>()

        val aktørId = AktørId("123456789")
        val fom = LocalDate.parse("2019-01-01")
        val tom = LocalDate.parse("2019-02-01")

        val expected = listOf(
                no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer(arbeidsgiver_organisasjon_3.orgnummer),
                        arbeidsgiver_organisasjon_3_navn)
        )

        every {
            arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom)
        } returns Try.Success(listOf(arbeidsforhold_med_arbeidsgiver_uten_navn))

        every {
            organisasjonService.hentOrganisasjon(Organisasjonsnummer(arbeidsgiver_organisasjon_3.orgnummer))
        } returns Either.Right(no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer(arbeidsgiver_organisasjon_3.orgnummer),
                arbeidsgiver_organisasjon_3_navn))

        val actual = ArbeidsgiverService(arbeidsforholdClient, organisasjonService)
                .finnArbeidsgivere(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(expected, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal returnere feil når oppslag av arbeidsforhold feiler`() {
        val arbeidsforholdClient = mockk<ArbeidsforholdClient>()
        val organisasjonService = mockk<OrganisasjonService>()

        val aktørId = AktørId("123456789")
        val fom = LocalDate.parse("2019-01-01")
        val tom = LocalDate.parse("2019-02-01")

        every {
            arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom)
        } returns Try.Failure(Exception("SOAP fault"))

        val actual = ArbeidsgiverService(arbeidsforholdClient, organisasjonService)
                .finnArbeidsgivere(aktørId, fom, tom)

        when (actual) {
            is Either.Left -> assertTrue(actual.a is Feilårsak.UkjentFeil)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal returnere tomt organisasjonnavn når oppslag av organisasjonnavn feiler`() {
        val arbeidsforholdClient = mockk<ArbeidsforholdClient>()
        val organisasjonService = mockk<OrganisasjonService>()

        val aktørId = AktørId("123456789")
        val fom = LocalDate.parse("2019-01-01")
        val tom = LocalDate.parse("2019-02-01")

        val expected = listOf(
                no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer(arbeidsgiver_organisasjon_3.orgnummer), null)
        )

        every {
            arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom)
        } returns Try.Success(listOf(arbeidsforhold_med_arbeidsgiver_uten_navn))

        every {
            organisasjonService.hentOrganisasjon(any())
        } returns Either.Left(Feilårsak.FeilFraTjeneste)

        val actual = ArbeidsgiverService(arbeidsforholdClient, organisasjonService)
                .finnArbeidsgivere(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(expected, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal fjerne duplikater ved flere arbeidsforhold i samme virksomhet`() {
        val arbeidsforholdClient = mockk<ArbeidsforholdClient>()
        val organisasjonService = mockk<OrganisasjonService>()

        val aktørId = AktørId("123456789")
        val fom = LocalDate.parse("2019-01-01")
        val tom = LocalDate.parse("2019-02-01")

        val expected = listOf(
                no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer(arbeidsgiver_organisasjon_1.orgnummer),
                        arbeidsgiver_organisasjon_1.navn)
        )

        every {
            arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom)
        } returns Try.Success(listOf(arbeidsforhold_uten_sluttdato, arbeidsforhold_uten_sluttdato))

        val actual = ArbeidsgiverService(arbeidsforholdClient, organisasjonService)
                .finnArbeidsgivere(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> assertEquals(expected, actual.b)
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal mappe sikkerhetsbegrensning til feilårsak for arbeidsgiveroppslag`() {
        val arbeidsforholdClient = mockk<ArbeidsforholdClient>()
        every {
            arbeidsforholdClient.finnArbeidsforhold(any(), any(), any())
        } returns Try.Failure(FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning("Fault", Sikkerhetsbegrensning()))


        val actual = ArbeidsgiverService(arbeidsforholdClient, mockk()).finnArbeidsgivere(
                AktørId("11987654321"), LocalDate.now(), LocalDate.now())

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.FeilFraTjeneste, actual.a)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal mappe ugyldig input til feilårsak for arbeidsgiveroppslag`() {
        val arbeidsforholdClient = mockk<ArbeidsforholdClient>()
        every {
            arbeidsforholdClient.finnArbeidsforhold(any(), any(), any())
        } returns Try.Failure(FinnArbeidsforholdPrArbeidstakerUgyldigInput("Fault", UgyldigInput()))

        val actual = ArbeidsgiverService(arbeidsforholdClient, mockk()).finnArbeidsgivere(
                AktørId("11987654321"), LocalDate.now(), LocalDate.now())

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.FeilFraTjeneste, actual.a)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal mappe exceptions til feilårsak for arbeidsgiveroppslag`() {
        val arbeidsforholdClient = mockk<ArbeidsforholdClient>()
        every {
            arbeidsforholdClient.finnArbeidsforhold(any(), any(), any())
        } returns Try.Failure(Exception("Fault"))

        val actual = ArbeidsgiverService(arbeidsforholdClient, mockk()).finnArbeidsgivere(
                AktørId("11987654321"), LocalDate.now(), LocalDate.now())

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.UkjentFeil, actual.a)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }
}

private val arbeidsgiver_organisasjon_1 = Organisasjon().apply {
    orgnummer = "889640782"
    navn = "S. VINDEL & SØNN"
}

private val arbeidsgiver_organisasjon_2 = Organisasjon().apply {
    orgnummer = "995298775"
    navn = "MATBUTIKKEN AS"
}

private val arbeidsgiver_organisasjon_3 = Organisasjon().apply {
    orgnummer = "912998827"
    navn = null
}

private val arbeidsgiver_organisasjon_3_navn = "MATBUTIKKEN AS"

private val arbeidsforholdID_for_arbeidsforhold_1 = 1234L
private val arbeidsforholdID_for_arbeidsforhold_2 = 5678L
private val arbeidsforholdID_for_arbeidsforhold_3 = 9123L

private val arbeidsforhold_uten_sluttdato get() = Arbeidsforhold().apply {
    arbeidsgiver = arbeidsgiver_organisasjon_1
    arbeidsforholdIDnav = arbeidsforholdID_for_arbeidsforhold_1
    ansettelsesPeriode = AnsettelsesPeriode().apply {
        periode = Gyldighetsperiode().apply {
            this.fom = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
        }
    }
    with(arbeidsavtale) {
        add(arbeidsforhold_uten_sluttdato_avtale)
    }
}

private val arbeidsforhold_uten_sluttdato_avtale get() = Arbeidsavtale().apply {
    fomGyldighetsperiode = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
    yrke = Yrker().apply {
        value = "Butikkmedarbeider"
    }
    stillingsprosent = BigDecimal.valueOf(100)
}

private val avsluttet_arbeidsforhold_med_permittering get() = Arbeidsforhold().apply {
    arbeidsgiver = arbeidsgiver_organisasjon_2
    arbeidsforholdIDnav = arbeidsforholdID_for_arbeidsforhold_2
    ansettelsesPeriode = AnsettelsesPeriode().apply {
        periode = Gyldighetsperiode().apply {
            this.fom = LocalDate.parse("2015-01-01").toXmlGregorianCalendar()
            this.tom = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
        }
    }
    with(arbeidsavtale) {
        add(avsluttet_arbeidsforhold_med_permittering_avtale)
    }
    with(permisjonOgPermittering) {
        add(permittering_for_avsluttet_arbeidsforhold_med_permittering)
    }
}

private val permittering_for_avsluttet_arbeidsforhold_med_permittering get() = PermisjonOgPermittering().apply {
    permisjonsPeriode = Gyldighetsperiode().apply {
        this.fom = LocalDate.parse("2016-01-01").toXmlGregorianCalendar()
        this.tom = LocalDate.parse("2016-01-02").toXmlGregorianCalendar()
        permisjonsprosent = BigDecimal.valueOf(100)
        permisjonOgPermittering = PermisjonsOgPermitteringsBeskrivelse().apply {
            value = "velferdspermisjon"
        }
    }
}

private val avsluttet_arbeidsforhold_med_permittering_avtale get() = Arbeidsavtale().apply {
    fomGyldighetsperiode = LocalDate.parse("2017-01-01").toXmlGregorianCalendar()
    tomGyldighetsperiode = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
    yrke = Yrker().apply {
        value = "Butikkmedarbeider"
    }
    stillingsprosent = BigDecimal.valueOf(100)
}
private val arbeidsforhold_med_arbeidsgiver_uten_navn get() = Arbeidsforhold().apply {
    arbeidsgiver = arbeidsgiver_organisasjon_3
    arbeidsforholdIDnav = arbeidsforholdID_for_arbeidsforhold_3
    ansettelsesPeriode = AnsettelsesPeriode().apply {
        periode = Gyldighetsperiode().apply {
            this.fom = LocalDate.parse("2019-01-01").toXmlGregorianCalendar()
        }
    }
    with(arbeidsavtale) {
        add(arbeidsforhold_uten_sluttdato_avtale)
    }
}
