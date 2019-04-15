package no.nav.helse.ws.inntekt.client

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Either
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.AktoerId
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektIdent
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektInformasjon
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektMaaned
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.math.BigDecimal
import java.time.YearMonth

class InntektClientTest {

    @Test
    fun `skal gi feil når oppslag gir feil`() {
        val aktørId = AktørId("11987654321")
        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val inntektV3 = mockk<InntektV3>()
        every {
            inntektV3.hentInntektListeBolk(any())
        } throws (Exception("SOAP fault"))

        val actual = InntektClient(inntektV3).hentBeregningsgrunnlag(aktørId, fom, tom)

        when (actual) {
            is Either.Left -> assertEquals("SOAP fault", actual.left.message)
            else -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal returnere liste over inntekter`() {
        val aktørId = AktørId("11987654321")
        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val formål = "Foreldrepenger"
        val filter = "8-28"

        val expected = HentInntektListeBolkResponse().apply {
            with (arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    ident = AktoerId().apply {
                        aktoerId = aktørId.aktor
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

        val inntektV3 = mockk<InntektV3>()
        every {
            inntektV3.hentInntektListeBolk(match {
                it.identListe.size == 1 && it.identListe[0] is AktoerId
                        && (it.identListe[0] as AktoerId).aktoerId == aktørId.aktor
                        && it.formaal.value == formål
                        && it.ainntektsfilter.value == filter
                        && it.uttrekksperiode.maanedFom == fom.toXmlGregorianCalendar()
                        && it .uttrekksperiode.maanedTom == tom.toXmlGregorianCalendar()
                })
        } returns HentInntektListeBolkResponse().apply {
            with (arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    ident = AktoerId().apply {
                        aktoerId = aktørId.aktor
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

        val actual = InntektClient(inntektV3).hentBeregningsgrunnlag(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> {
                assertEquals(expected.arbeidsInntektIdentListe.size, actual.right.arbeidsInntektIdentListe.size)
                expected.arbeidsInntektIdentListe.forEachIndexed { index, arbeidsInntektIdent ->
                    val actualArbeidsInntektIdent = actual.right.arbeidsInntektIdentListe[index]

                    assertTrue(actualArbeidsInntektIdent.ident is AktoerId)
                    assertEquals((arbeidsInntektIdent.ident as AktoerId).aktoerId, (actualArbeidsInntektIdent.ident as AktoerId).aktoerId)

                    assertEquals(arbeidsInntektIdent.arbeidsInntektMaaned.size, actualArbeidsInntektIdent.arbeidsInntektMaaned.size)

                    arbeidsInntektIdent.arbeidsInntektMaaned.forEachIndexed { maanedIdx, arbeidsInntektMaaned ->
                        val actualArbeidsInntektMaaned = actualArbeidsInntektIdent.arbeidsInntektMaaned[maanedIdx]

                        assertEquals(arbeidsInntektMaaned.aarMaaned, actualArbeidsInntektMaaned.aarMaaned)
                        assertEquals(arbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe.size, actualArbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe.size)

                        arbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe.forEachIndexed { inntektlisteIdx, inntekt ->
                            val actualInntekt = actualArbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe[inntektlisteIdx]

                            assertEquals(inntekt.beloep, actualInntekt.beloep)
                        }
                    }
                }
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }

    }

    @Test
    fun `skal gi feil når oppslag gir feil for sammenligningsgrunnlag`() {
        val aktørId = AktørId("11987654321")
        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val inntektV3 = mockk<InntektV3>()
        every {
            inntektV3.hentInntektListeBolk(any())
        } throws (Exception("SOAP fault"))

        val actual = InntektClient(inntektV3).hentSammenligningsgrunnlag(aktørId, fom, tom)

        when (actual) {
            is Either.Left -> assertEquals("SOAP fault", actual.left.message)
            else -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal returnere liste over inntekter for sammenligningsgrunnlag`() {
        val aktørId = AktørId("11987654321")
        val fom = YearMonth.parse("2019-01")
        val tom = YearMonth.parse("2019-02")

        val formål = "Foreldrepenger"
        val filter = "8-30"

        val expected = HentInntektListeBolkResponse().apply {
            with (arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    ident = AktoerId().apply {
                        aktoerId = aktørId.aktor
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

        val inntektV3 = mockk<InntektV3>()
        every {
            inntektV3.hentInntektListeBolk(match {
                it.identListe.size == 1 && it.identListe[0] is AktoerId
                        && (it.identListe[0] as AktoerId).aktoerId == aktørId.aktor
                        && it.formaal.value == formål
                        && it.ainntektsfilter.value == filter
                        && it.uttrekksperiode.maanedFom == fom.toXmlGregorianCalendar()
                        && it .uttrekksperiode.maanedTom == tom.toXmlGregorianCalendar()
            })
        } returns HentInntektListeBolkResponse().apply {
            with (arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    ident = AktoerId().apply {
                        aktoerId = aktørId.aktor
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

        val actual = InntektClient(inntektV3).hentSammenligningsgrunnlag(aktørId, fom, tom)

        when (actual) {
            is Either.Right -> {
                assertEquals(expected.arbeidsInntektIdentListe.size, actual.right.arbeidsInntektIdentListe.size)
                expected.arbeidsInntektIdentListe.forEachIndexed { index, arbeidsInntektIdent ->
                    val actualArbeidsInntektIdent = actual.right.arbeidsInntektIdentListe[index]

                    assertTrue(actualArbeidsInntektIdent.ident is AktoerId)
                    assertEquals((arbeidsInntektIdent.ident as AktoerId).aktoerId, (actualArbeidsInntektIdent.ident as AktoerId).aktoerId)

                    assertEquals(arbeidsInntektIdent.arbeidsInntektMaaned.size, actualArbeidsInntektIdent.arbeidsInntektMaaned.size)

                    arbeidsInntektIdent.arbeidsInntektMaaned.forEachIndexed { maanedIdx, arbeidsInntektMaaned ->
                        val actualArbeidsInntektMaaned = actualArbeidsInntektIdent.arbeidsInntektMaaned[maanedIdx]

                        assertEquals(arbeidsInntektMaaned.aarMaaned, actualArbeidsInntektMaaned.aarMaaned)
                        assertEquals(arbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe.size, actualArbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe.size)

                        arbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe.forEachIndexed { inntektlisteIdx, inntekt ->
                            val actualInntekt = actualArbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe[inntektlisteIdx]

                            assertEquals(inntekt.beloep, actualInntekt.beloep)
                        }
                    }
                }
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }

    }
}
