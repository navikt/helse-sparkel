package no.nav.helse.ws.inntekt

import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektIdent
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektInformasjon
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektMaaned
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.PersonIdent
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
            inntektClient.hentInntektListe(aktør, fom, tom)
        } returns OppslagResult.Feil(HttpStatusCode.InternalServerError, Feil.Exception("SOAP fault", Exception("SOAP fault")))

        val actual = InntektService(inntektClient).hentInntekter(aktør, fom, tom)

        when (actual) {
            is OppslagResult.Feil -> {
                when (actual.feil) {
                    is Feil.Exception -> {
                        assertEquals(HttpStatusCode.InternalServerError, actual.httpCode)
                        assertEquals("SOAP fault", (actual.feil as Feil.Exception).feilmelding)
                    }
                    else -> fail { "Expected Feil.Exception to be returned" }
                }
            }
            else -> fail { "Expected OppslagResult.Feil to be returned" }
        }
    }

    @Test
    fun `skal returnere liste over inntekter`() {
        val aktør = AktørId("11987654321")

        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val expected = HentInntektListeBolkResponse().apply {
            with (arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    ident = PersonIdent().apply {
                        personIdent = aktør.aktor
                    }
                    with (arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            aarMaaned = fom.toXmlGregorianCalendar()
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with (inntektListe) {
                                    add(Inntekt().apply {
                                        beloep = BigDecimal.valueOf(2500)
                                    })
                                }
                            }
                        })
                    }
                })
            }
        }

        val inntektClient = mockk<InntektClient>()
        every {
            inntektClient.hentInntektListe(aktør, fom, tom)
        } returns HentInntektListeBolkResponse().apply {
            with (arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    ident = PersonIdent().apply {
                        personIdent = aktør.aktor
                    }
                    with (arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            aarMaaned = fom.toXmlGregorianCalendar()
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with (inntektListe) {
                                    add(Inntekt().apply {
                                        beloep = BigDecimal.valueOf(2500)
                                    })
                                }
                            }
                        })
                    }
                })
            }
        }.let {
            OppslagResult.Ok(it)
        }

        val actual = InntektService(inntektClient).hentInntekter(aktør, fom, tom)

        when (actual) {
            is OppslagResult.Ok -> {
                assertEquals(expected.arbeidsInntektIdentListe.size, actual.data.arbeidsInntektIdentListe.size)
                expected.arbeidsInntektIdentListe.forEachIndexed { index, arbeidsInntektIdent ->
                    val actualArbeidsInntektIdent = actual.data.arbeidsInntektIdentListe[index]

                    assertTrue(actualArbeidsInntektIdent.ident is PersonIdent)
                    assertEquals((arbeidsInntektIdent.ident as PersonIdent).personIdent, (actualArbeidsInntektIdent.ident as PersonIdent).personIdent)

                    assertEquals(arbeidsInntektIdent.arbeidsInntektMaaned.size, actualArbeidsInntektIdent.arbeidsInntektMaaned.size)

                    arbeidsInntektIdent.arbeidsInntektMaaned.forEachIndexed { index, arbeidsInntektMaaned ->
                        val actualArbeidsInntektMaaned = actualArbeidsInntektIdent.arbeidsInntektMaaned[index]

                        assertEquals(arbeidsInntektMaaned.aarMaaned, actualArbeidsInntektMaaned.aarMaaned)
                        assertEquals(arbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe.size, actualArbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe.size)

                        arbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe.forEachIndexed { index, inntekt ->
                            val actualInntekt = actualArbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe[index]

                            assertEquals(inntekt.beloep, actualInntekt.beloep)
                        }
                    }
                }
            }
            is OppslagResult.Feil -> fail { "Expected OppslagResult.Ok to be returned" }
        }

    }
}
