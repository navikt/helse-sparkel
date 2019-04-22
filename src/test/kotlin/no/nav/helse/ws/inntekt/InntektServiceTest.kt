package no.nav.helse.ws.inntekt

import arrow.core.Either
import arrow.core.Try
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Feilårsak
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.inntekt.client.InntektClient
import no.nav.helse.ws.inntekt.domain.Virksomhet
import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*
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
            inntektClient.hentInntekter(aktør, fom, tom, "ForeldrepengerA-inntekt")
        } returns Try.Failure(Exception("SOAP fault"))

        val actual = InntektService(inntektClient).hentInntekter(aktør, fom, tom, "ForeldrepengerA-inntekt")

        when (actual) {
            is Either.Left -> assertTrue(actual.a is Feilårsak.UkjentFeil)
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

        val inntektClient = mockk<InntektClient>()
        every {
            inntektClient.hentInntekter(aktør, fom, tom, "ForeldrepengerA-inntekt")
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
                                    add(Loennsinntekt().apply {
                                        beloep = BigDecimal.valueOf(2500)
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktør.aktor
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "889640782"
                                        }
                                        utbetaltIPeriode = fom.toXmlGregorianCalendar()
                                    })
                                    add(YtelseFraOffentlige().apply {
                                        beloep = BigDecimal.valueOf(500)
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktør.aktor
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "995277670"
                                        }
                                        utbetaltIPeriode = fom.toXmlGregorianCalendar()
                                        beskrivelse = YtelseFraOffentligeBeskrivelse().apply {
                                            value = "barnetrygd"
                                        }
                                    })
                                    add(Naeringsinntekt().apply {
                                        beloep = BigDecimal.valueOf(1500)
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktør.aktor
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "889640782"
                                        }
                                        utbetaltIPeriode = fom.toXmlGregorianCalendar()
                                        beskrivelse = Naeringsinntektsbeskrivelse().apply {
                                            value = "næringsinntekt"
                                        }
                                    })
                                    add(PensjonEllerTrygd().apply {
                                        beloep = BigDecimal.valueOf(3000)
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktør.aktor
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "995277670"
                                        }
                                        utbetaltIPeriode = fom.toXmlGregorianCalendar()
                                        beskrivelse = PensjonEllerTrygdebeskrivelse().apply {
                                            value = "alderspensjon"
                                        }
                                    })
                                }
                            }
                        })
                    }
                })
            }
        }.let {
            Try.Success(it.arbeidsInntektIdentListe)
        }

        val actual = InntektService(inntektClient).hentInntekter(aktør, fom, tom, "ForeldrepengerA-inntekt")

        when (actual) {
            is Either.Right -> {
                assertEquals(expected.size, actual.b.size)
                expected.forEachIndexed { index, inntekt ->
                    assertEquals(inntekt, actual.b[index])
                }
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }

    }

    @Test
    fun `skal gi feil når tjenesten svarer med sikkerhetsavvik`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val inntektClient = mockk<InntektClient>()
        every {
            inntektClient.hentInntekter(aktør, fom, tom, "ForeldrepengerA-inntekt")
        } returns HentInntektListeBolkResponse().apply {
            with (sikkerhetsavvikListe) {
                add(Sikkerhetsavvik().apply {
                    tekst = "en feil nr 1"
                })
                add(Sikkerhetsavvik().apply {
                    tekst = "en feil nr 2"
                })
            }
        }.let {
            Try.Failure(SikkerhetsavvikException("en feil nr 1, en feil nr 2"))
        }

        val actual = InntektService(inntektClient).hentInntekter(aktør, fom, tom, "ForeldrepengerA-inntekt")

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.FeilFraTjeneste, actual.a)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }

    }
}
