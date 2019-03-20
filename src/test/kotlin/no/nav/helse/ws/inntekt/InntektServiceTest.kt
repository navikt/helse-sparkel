package no.nav.helse.ws.inntekt

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Either
import no.nav.helse.Feilårsak
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.organisasjon.OrganisasjonService
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.AktoerId
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektIdent
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektInformasjon
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektMaaned
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Periode
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.math.BigDecimal
import java.time.YearMonth

class InntektServiceTest {

    @Test
    fun `skal gi feil når oppslag gir feil`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val inntektClient = mockk<InntektClient>()
        every {
            inntektClient.hentBeregningsgrunnlag(aktør, fom, tom)
        } returns Either.Left(Exception("SOAP fault"))

        val actual = InntektService(inntektClient, mockk()).hentBeregningsgrunnlag(aktør, fom, tom)

        when (actual) {
            is Either.Left -> assertTrue(actual.left is Feilårsak.UkjentFeil)
            else -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal returnere liste over inntekter`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val expected = listOf(
                no.nav.helse.ws.inntekt.Inntekt(Arbeidsgiver.Organisasjon("12345"),
                        Opptjeningsperiode(fom.atDay(1), fom.atEndOfMonth()),
                        BigDecimal.valueOf(2500))
        )

        val inntektClient = mockk<InntektClient>()
        every {
            inntektClient.hentBeregningsgrunnlag(aktør, fom, tom)
        } returns HentInntektListeBolkResponse().apply {
            with (arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    ident = AktoerId().apply {
                        aktoerId = aktør.aktor
                    }
                    with (arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            aarMaaned = fom.toXmlGregorianCalendar()
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with (inntektListe) {
                                    add(Inntekt().apply {
                                        beloep = BigDecimal.valueOf(2500)
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktør.aktor
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "12345"
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.atDay(1).toXmlGregorianCalendar()
                                            sluttDato = fom.atEndOfMonth().toXmlGregorianCalendar()
                                        }
                                    })
                                }
                            }
                        })
                    }
                })
            }
        }.let {
            Either.Right(it)
        }

        val organisasjonService = mockk<OrganisasjonService>()

        every {
            organisasjonService.hentOrganisasjon(match {
                it.value == "12345"
            })
        } returns Either.Right(no.nav.helse.ws.organisasjon.Organisasjon("12345", no.nav.helse.ws.organisasjon.Organisasjon.Type.Virksomhet, null))

        val actual = InntektService(inntektClient, organisasjonService).hentBeregningsgrunnlag(aktør, fom, tom)

        when (actual) {
            is Either.Right -> {
                assertEquals(expected.size, actual.right.size)
                expected.forEachIndexed { index, inntekt ->
                    assertEquals(inntekt, actual.right[index])
                }
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }

    }

    @Test
    fun `skal gi feil når oppslag gir feil for sammenligningsgrunnlag`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val inntektClient = mockk<InntektClient>()
        every {
            inntektClient.hentSammenligningsgrunnlag(aktør, fom, tom)
        } returns Either.Left(Exception("SOAP fault"))

        val actual = InntektService(inntektClient, mockk()).hentSammenligningsgrunnlag(aktør, fom, tom)

        when (actual) {
            is Either.Left -> assertTrue(actual.left is Feilårsak.UkjentFeil)
            else -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal returnere liste over inntekter for sammenligningsgrunnlag`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val expected = listOf(
                no.nav.helse.ws.inntekt.Inntekt(Arbeidsgiver.Organisasjon("12345"),
                        Opptjeningsperiode(fom.atDay(1), fom.atEndOfMonth()),
                        BigDecimal.valueOf(2500))
        )

        val inntektClient = mockk<InntektClient>()
        every {
            inntektClient.hentSammenligningsgrunnlag(aktør, fom, tom)
        } returns HentInntektListeBolkResponse().apply {
            with (arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    ident = AktoerId().apply {
                        aktoerId = aktør.aktor
                    }
                    with (arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            aarMaaned = fom.toXmlGregorianCalendar()
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with (inntektListe) {
                                    add(Inntekt().apply {
                                        beloep = BigDecimal.valueOf(2500)
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktør.aktor
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "12345"
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.atDay(1).toXmlGregorianCalendar()
                                            sluttDato = fom.atEndOfMonth().toXmlGregorianCalendar()
                                        }
                                    })
                                }
                            }
                        })
                    }
                })
            }
        }.let {
            Either.Right(it)
        }

        val organisasjonService = mockk<OrganisasjonService>()

        every {
            organisasjonService.hentOrganisasjon(match {
                it.value == "12345"
            })
        } returns Either.Right(no.nav.helse.ws.organisasjon.Organisasjon("12345", no.nav.helse.ws.organisasjon.Organisasjon.Type.Virksomhet, null))

        val actual = InntektService(inntektClient, organisasjonService).hentSammenligningsgrunnlag(aktør, fom, tom)

        when (actual) {
            is Either.Right -> {
                assertEquals(expected.size, actual.right.size)
                expected.forEachIndexed { index, inntekt ->
                    assertEquals(inntekt, actual.right[index])
                }
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }

    }

    @Test
    fun `skal hente virksomhetsnr for juridisk enhet`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val expected = listOf(
                no.nav.helse.ws.inntekt.Inntekt(Arbeidsgiver.Organisasjon("12345"),
                        Opptjeningsperiode(fom.atDay(1), fom.atEndOfMonth()),
                        BigDecimal.valueOf(2500))
        )

        val inntektClient = mockk<InntektClient>()
        every {
            inntektClient.hentSammenligningsgrunnlag(aktør, fom, tom)
        } returns HentInntektListeBolkResponse().apply {
            with (arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    ident = AktoerId().apply {
                        aktoerId = aktør.aktor
                    }
                    with (arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            aarMaaned = fom.toXmlGregorianCalendar()
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with (inntektListe) {
                                    add(Inntekt().apply {
                                        beloep = BigDecimal.valueOf(2500)
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktør.aktor
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "12345"
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.atDay(1).toXmlGregorianCalendar()
                                            sluttDato = fom.atEndOfMonth().toXmlGregorianCalendar()
                                        }
                                    })
                                }
                            }
                        })
                    }
                })
            }
        }.let {
            Either.Right(it)
        }

        val organisasjonService = mockk<OrganisasjonService>()

        every {
            organisasjonService.hentOrganisasjon(match {
                it.value == "12345"
            })
        } returns Either.Right(no.nav.helse.ws.organisasjon.Organisasjon("12345", no.nav.helse.ws.organisasjon.Organisasjon.Type.Virksomhet, null))

        val actual = InntektService(inntektClient, organisasjonService).hentSammenligningsgrunnlag(aktør, fom, tom)

        when (actual) {
            is Either.Right -> {
                assertEquals(expected.size, actual.right.size)
                expected.forEachIndexed { index, inntekt ->
                    assertEquals(inntekt, actual.right[index])
                }
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal gi feil om oppslag for virksomhetsnr for juridisk enhet gir feil`() {

    }
}
