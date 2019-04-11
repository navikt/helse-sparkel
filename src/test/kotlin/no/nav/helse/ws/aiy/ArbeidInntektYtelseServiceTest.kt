package no.nav.helse.ws.aiy

import io.mockk.every
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.helse.Either
import no.nav.helse.Feilårsak
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdService
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsgiver
import no.nav.helse.ws.inntekt.InntektService
import no.nav.helse.ws.inntekt.domain.ArbeidsforholdFrilanser
import no.nav.helse.ws.inntekt.domain.Inntekt
import no.nav.helse.ws.inntekt.domain.Virksomhet
import no.nav.helse.ws.organisasjon.OrganisasjonService
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

        val virksomhet1 = Organisasjonsnummer("889640782")
        val virksomhet2 = Organisasjonsnummer("995298775")
        val frilansVirksomhet1 = Organisasjonsnummer("971524960")

        val arbeidsforholdliste = listOf(
                Arbeidsforhold(Arbeidsgiver.Virksomhet(Organisasjon.Virksomhet(virksomhet1, "S. VINDEL & SØNN")), fom),
                Arbeidsforhold(Arbeidsgiver.Virksomhet(Organisasjon.Virksomhet(virksomhet2, "Matbutikken A/S")), LocalDate.parse("2018-01-01"), fom)
        )
        val frilansArbeidsforhold = listOf(
                ArbeidsforholdFrilanser(Virksomhet.Organisasjon(frilansVirksomhet1), LocalDate.parse("2019-01-01"), null, null)
        )

        val inntekter = listOf(
                Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 1), BigDecimal(20000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 2), BigDecimal(25000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 10), BigDecimal(15000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 11), BigDecimal(16000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 12), BigDecimal(17000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(frilansVirksomhet1), YearMonth.of(2019, 2), BigDecimal(30000))
        )

        val expected = ArbeidInntektYtelse(
                arbeidsforhold = mapOf(
                        no.nav.helse.ws.aiy.domain.Arbeidsforhold.Arbeidstaker(Virksomhet.Organisasjon(virksomhet1), fom) to mapOf(
                                YearMonth.of(2019, 1) to listOf(
                                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 1), BigDecimal(20000))
                                ),

                                YearMonth.of(2019, 2) to listOf(
                                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 2), BigDecimal(25000))
                                )
                        ),
                        no.nav.helse.ws.aiy.domain.Arbeidsforhold.Arbeidstaker(Virksomhet.Organisasjon(virksomhet2), LocalDate.parse("2018-01-01"), fom) to mapOf(
                                YearMonth.of(2018, 10) to listOf(
                                    Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 10), BigDecimal(15000))
                                ),
                                YearMonth.of(2018, 11) to listOf(
                                    Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 11), BigDecimal(16000))
                                ),
                                YearMonth.of(2018, 12) to listOf(
                                    Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 12), BigDecimal(17000))
                                )
                        ),
                        no.nav.helse.ws.aiy.domain.Arbeidsforhold.Frilans(Virksomhet.Organisasjon(frilansVirksomhet1), LocalDate.parse("2019-01-01")) to mapOf(
                                YearMonth.of(2019, 2) to listOf(
                                        Inntekt.Lønn(Virksomhet.Organisasjon(frilansVirksomhet1), YearMonth.of(2019, 2), BigDecimal(30000))
                                )
                        )
                )
        )

        val arbeidsforholdService = mockk<ArbeidsforholdService>()
        val inntektService = mockk<InntektService>()
        val organisasjonService = mockk<OrganisasjonService>()

        val aktiveArbeidsforholdService = ArbeidInntektYtelseService(arbeidsforholdService, inntektService, organisasjonService)

        val arbeidsforholdAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals", arrayOf("type"), arrayOf("Arbeidstaker")) ?: 0.0
        val inntektAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(arbeidsforholdliste)

        every {
            inntektService.hentInntekter(aktørId, YearMonth.from(fom), YearMonth.from(tom))
        } returns Either.Right(inntekter)

        every {
            inntektService.hentFrilansarbeidsforhold(aktørId, YearMonth.from(fom), YearMonth.from(tom))
        } returns Either.Right(frilansArbeidsforhold)

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        val arbeidsforholdAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals", arrayOf("type"), arrayOf("Arbeidstaker")) ?: 0.0
        val inntektAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        assertEquals(0.0, arbeidsforholdAvviksCounterAfter - arbeidsforholdAvviksCounterBefore)
        assertEquals(0.0, inntektAvviksCounterAfter - inntektAvviksCounterBefore)

        when (actual) {
            is Either.Right -> {
                assertEquals(3, actual.right.arbeidsforhold.size)
                assertEquals(expected, actual.right)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal gruppere ytelser og trygd`() {
        val aktørId = AktørId("123456789")
        val fom = LocalDate.parse("2019-01-01")
        val tom = LocalDate.parse("2019-03-01")

        val virksomhet1 = Organisasjonsnummer("995277670")
        val virksomhet2 = Organisasjonsnummer("958995369")

        val arbeidsforholdliste = emptyList<Arbeidsforhold>()
        val frilansArbeidsforhold = emptyList<ArbeidsforholdFrilanser>()

        val inntekter = listOf(
                Inntekt.Ytelse(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 1), BigDecimal(20000), "foreldrepenger"),
                Inntekt.Ytelse(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 2), BigDecimal(25000), "sykepenger"),
                Inntekt.PensjonEllerTrygd(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 10), BigDecimal(15000), "ufoerepensjonFraAndreEnnFolketrygden"),
                Inntekt.PensjonEllerTrygd(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 11), BigDecimal(16000), "ufoerepensjonFraAndreEnnFolketrygden")
        )

        val expected = ArbeidInntektYtelse(
                ytelser = listOf(
                        Inntekt.Ytelse(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 1), BigDecimal(20000), "foreldrepenger"),
                        Inntekt.Ytelse(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 2), BigDecimal(25000), "sykepenger")
                ),
                pensjonEllerTrygd = listOf(
                        Inntekt.PensjonEllerTrygd(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 10), BigDecimal(15000), "ufoerepensjonFraAndreEnnFolketrygden"),
                        Inntekt.PensjonEllerTrygd(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 11), BigDecimal(16000), "ufoerepensjonFraAndreEnnFolketrygden")
                )
        )

        val arbeidsforholdService = mockk<ArbeidsforholdService>()
        val inntektService = mockk<InntektService>()
        val organisasjonService = mockk<OrganisasjonService>()

        val aktiveArbeidsforholdService = ArbeidInntektYtelseService(arbeidsforholdService, inntektService, organisasjonService)

        every {
            arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom)
        } returns Either.Right(arbeidsforholdliste)

        every {
            inntektService.hentInntekter(aktørId, YearMonth.from(fom), YearMonth.from(tom))
        } returns Either.Right(inntekter)

        every {
            inntektService.hentFrilansarbeidsforhold(aktørId, YearMonth.from(fom), YearMonth.from(tom))
        } returns Either.Right(frilansArbeidsforhold)

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> {
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
                        no.nav.helse.ws.aiy.domain.Arbeidsforhold.Arbeidstaker(Virksomhet.Organisasjon(virksomhet1), fom) to mapOf(
                                YearMonth.of(2019, 1) to listOf(
                                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 1), BigDecimal(20000))
                                ),

                                YearMonth.of(2019, 2) to listOf(
                                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet1), YearMonth.of(2019, 2), BigDecimal(25000))
                                )
                        )
                )
        )

        val arbeidsforholdService = mockk<ArbeidsforholdService>()
        val inntektService = mockk<InntektService>()
        val organisasjonService = mockk<OrganisasjonService>()

        val aktiveArbeidsforholdService = ArbeidInntektYtelseService(arbeidsforholdService, inntektService, organisasjonService)

        val arbeidsforholdAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals", arrayOf("type"), arrayOf("Arbeidstaker")) ?: 0.0
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

        every {
            organisasjonService.hentOrganisasjon(Organisasjonsnummer("995298775"))
        } returns Either.Right(Organisasjon.JuridiskEnhet(Organisasjonsnummer("995298775")))

        every {
            organisasjonService.hentVirksomhetForJuridiskOrganisasjonsnummer(Organisasjonsnummer("995298775"), LocalDate.of(2018, 10, 1))
        } returns Either.Left(Feilårsak.IkkeFunnet)

        every {
            organisasjonService.hentVirksomhetForJuridiskOrganisasjonsnummer(Organisasjonsnummer("995298775"), LocalDate.of(2018, 11, 1))
        } returns Either.Left(Feilårsak.IkkeFunnet)

        every {
            organisasjonService.hentVirksomhetForJuridiskOrganisasjonsnummer(Organisasjonsnummer("995298775"), LocalDate.of(2018, 12, 1))
        } returns Either.Left(Feilårsak.IkkeFunnet)

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        val arbeidsforholdAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals", arrayOf("type"), arrayOf("Arbeidstaker")) ?: 0.0
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
                        no.nav.helse.ws.aiy.domain.Arbeidsforhold.Arbeidstaker(Virksomhet.Organisasjon(virksomhet2), LocalDate.parse("2018-01-01"), fom) to mapOf(
                                YearMonth.of(2018, 10) to listOf(
                                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 10), BigDecimal(15000))
                                ),
                                YearMonth.of(2018, 11) to listOf(
                                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 11), BigDecimal(16000))
                                ),
                                YearMonth.of(2018, 12) to listOf(
                                        Inntekt.Lønn(Virksomhet.Organisasjon(virksomhet2), YearMonth.of(2018, 12), BigDecimal(17000))
                                )
                        )
                )
        )

        val arbeidsforholdService = mockk<ArbeidsforholdService>()
        val inntektService = mockk<InntektService>()
        val organisasjonService = mockk<OrganisasjonService>()

        val aktiveArbeidsforholdService = ArbeidInntektYtelseService(arbeidsforholdService, inntektService, organisasjonService)

        val arbeidsforholdAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals", arrayOf("type"), arrayOf("Arbeidstaker")) ?: 0.0
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

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        val arbeidsforholdAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals", arrayOf("type"), arrayOf("Arbeidstaker")) ?: 0.0
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

    @Test
    fun `skal sammenstille inntekter rapportert på jurdisk nummer med arbeidsforhold rapporter på virksomhetsnummer`() {
        val aktørId = AktørId("123456789")
        val fom = LocalDate.parse("2019-01-01")
        val tom = LocalDate.parse("2019-03-01")

        val arbeidsforholdliste = listOf(
                Arbeidsforhold(Arbeidsgiver.Virksomhet(Organisasjon.Virksomhet(Organisasjonsnummer("995298775"), "ARBEIDS- OG VELFERDSDIREKTORATET AVD SANNERGATA")), fom)
        )

        val inntekter = listOf(
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), YearMonth.of(2018, 10), BigDecimal(15000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")), YearMonth.of(2018, 11), BigDecimal(16000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")), YearMonth.of(2018, 12), BigDecimal(17000))
        )

        val expected = ArbeidInntektYtelse(
                arbeidsforhold = mapOf(
                        no.nav.helse.ws.aiy.domain.Arbeidsforhold.Arbeidstaker(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), fom) to mapOf(
                                YearMonth.of(2018, 10) to listOf(
                                        Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), YearMonth.of(2018, 10), BigDecimal(15000))
                                ),
                                YearMonth.of(2018, 11) to listOf(
                                        Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), YearMonth.of(2018, 11), BigDecimal(16000))
                                ),
                                YearMonth.of(2018, 12) to listOf(
                                        Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("995298775")), YearMonth.of(2018, 12), BigDecimal(17000))
                                )
                        )
                )
        )

        val organisasjonService = mockk<OrganisasjonService>()

        every {
            organisasjonService.hentOrganisasjon(match {
                it.value == "889640782"
            })
        } returns Either.Right(no.nav.helse.ws.organisasjon.domain.Organisasjon.JuridiskEnhet(Organisasjonsnummer("889640782"), "ARBEIDS- OG VELFERDSETATEN"))

        every {
            organisasjonService.hentVirksomhetForJuridiskOrganisasjonsnummer(match {
                it.value == "889640782"
            }, match {
               it == LocalDate.of(2018, 11, 1)
                       || it == LocalDate.of(2018, 12, 1)
            })
        } returns Either.Right(Organisasjonsnummer("995298775"))

        val arbeidsforholdService = mockk<ArbeidsforholdService>()
        val inntektService = mockk<InntektService>()

        val aktiveArbeidsforholdService = ArbeidInntektYtelseService(arbeidsforholdService, inntektService, organisasjonService)

        val arbeidsforholdAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals", arrayOf("type"), arrayOf("Arbeidstaker")) ?: 0.0
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

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        val arbeidsforholdAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals", arrayOf("type"), arrayOf("Arbeidstaker")) ?: 0.0
        val inntektAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        assertEquals(0.0, arbeidsforholdAvviksCounterAfter - arbeidsforholdAvviksCounterBefore)
        assertEquals(0.0, inntektAvviksCounterAfter - inntektAvviksCounterBefore)

        when (actual) {
            is Either.Right -> {
                assertEquals(expected, actual.right)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal telle avvik når oppslag for virksomhetsnr for juridisk enhet ender i feil, og vi ikke kan mappe til arbeidsforhold`() {
        val aktørId = AktørId("123456789")
        val fom = LocalDate.parse("2019-01-01")
        val tom = LocalDate.parse("2019-03-01")

        val arbeidsforholdliste = listOf(
                Arbeidsforhold(Arbeidsgiver.Virksomhet(Organisasjon.Virksomhet(Organisasjonsnummer("995298775"), "ARBEIDS- OG VELFERDSDIREKTORATET AVD SANNERGATA")), fom)
        )

        val inntekter = listOf(
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")), YearMonth.of(2018, 10), BigDecimal(15000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")), YearMonth.of(2018, 11), BigDecimal(16000)),
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")), YearMonth.of(2018, 12), BigDecimal(17000))
        )

        val expected = ArbeidInntektYtelse(
                arbeidsforhold = emptyMap()
        )

        val organisasjonService = mockk<OrganisasjonService>()

        every {
            organisasjonService.hentOrganisasjon(match {
                it.value == "889640782"
            })
        } returns Either.Right(no.nav.helse.ws.organisasjon.domain.Organisasjon.JuridiskEnhet(Organisasjonsnummer("889640782"), "ARBEIDS- OG VELFERDSETATEN"))

        every {
            organisasjonService.hentVirksomhetForJuridiskOrganisasjonsnummer(match {
                it.value == "889640782"
            }, match {
                it == LocalDate.of(2018, 10, 1)
                        || it == LocalDate.of(2018, 11, 1)
                        || it == LocalDate.of(2018, 12, 1)
            })
        } returns Either.Left(Feilårsak.FeilFraTjeneste)

        val arbeidsforholdService = mockk<ArbeidsforholdService>()
        val inntektService = mockk<InntektService>()

        val aktiveArbeidsforholdService = ArbeidInntektYtelseService(arbeidsforholdService, inntektService, organisasjonService)

        val arbeidsforholdAvviksCounterBefore = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals", arrayOf("type"), arrayOf("Arbeidstaker")) ?: 0.0
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

        val actual = aktiveArbeidsforholdService.finnArbeidInntekterOgYtelser(aktørId, fom, tom)

        val arbeidsforholdAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("arbeidsforhold_avvik_totals", arrayOf("type"), arrayOf("Arbeidstaker")) ?: 0.0
        val inntektAvviksCounterAfter = CollectorRegistry.defaultRegistry.getSampleValue("inntekt_avvik_totals")

        assertEquals(1.0, arbeidsforholdAvviksCounterAfter - arbeidsforholdAvviksCounterBefore)
        assertEquals(3.0, inntektAvviksCounterAfter - inntektAvviksCounterBefore)

        when (actual) {
            is Either.Right -> {
                assertEquals(expected, actual.right)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }
}
