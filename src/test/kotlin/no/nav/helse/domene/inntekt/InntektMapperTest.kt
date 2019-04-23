package no.nav.helse.domene.inntekt

import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.inntekt.domain.Inntekt
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.inntekt.domain.Virksomhet
import no.nav.helse.domene.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class InntektMapperTest {

    @Test
    fun `skal fjerne andre aktører enn den man spør om`() {
        val aktørId = AktørId("11987654321")
        val tom = YearMonth.now()
        val fom = tom.minusMonths(1)

        val expected = listOf(
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(1500))
        )

        val inntekter = HentInntektListeBolkResponse().apply {
            with(arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    with(arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with(inntektListe) {
                                    add(Loennsinntekt().apply {
                                        inntektsmottaker = PersonIdent().apply {
                                            personIdent = "12345678911"
                                        }
                                    })
                                    add(Loennsinntekt().apply {
                                        inntektsmottaker = Organisasjon().apply {
                                            orgnummer = "11223344"
                                        }
                                    })
                                    add(Loennsinntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        utbetaltIPeriode = fom.toXmlGregorianCalendar()
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "889640782"
                                        }
                                        beloep = BigDecimal.valueOf(1500)
                                    })
                                }
                            }
                        })
                    }
                })
            }
        }

        val actual = InntektMapper.mapToInntekt(aktørId, fom, tom, inntekter.arbeidsInntektIdentListe)

        assertEquals(expected.size, actual.size)
        expected.forEachIndexed { key, inntekt ->
            assertEquals(inntekt, actual[key])
        }
    }

    @Test
    fun `skal mappe forskjellige inntektstyper`() {
        val aktørId = AktørId("11987654321")
        val tom = YearMonth.now()
        val fom = tom.minusMonths(1)

        val expected = listOf(
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(2500)),
                Inntekt.Ytelse(Virksomhet.Organisasjon(Organisasjonsnummer("995277670")),
                        fom, BigDecimal.valueOf(500), "barnetrygd"),
                Inntekt.Næring(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(1500), "næringsinntekt"),
                Inntekt.PensjonEllerTrygd(Virksomhet.Organisasjon(Organisasjonsnummer("995277670")),
                        fom, BigDecimal.valueOf(3000), "alderspensjon")
        )
        val inntekter = HentInntektListeBolkResponse().apply {
            with(arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    with(arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with(inntektListe) {
                                    add(Loennsinntekt().apply {
                                        beloep = BigDecimal.valueOf(2500)
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "889640782"
                                        }
                                        utbetaltIPeriode = fom.toXmlGregorianCalendar()
                                    })
                                    add(YtelseFraOffentlige().apply {
                                        beloep = BigDecimal.valueOf(500)
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
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
                                            aktoerId = aktørId.aktor
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
                                            aktoerId = aktørId.aktor
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
        }

        val actual = InntektMapper.mapToInntekt(aktørId, fom, tom, inntekter.arbeidsInntektIdentListe)

        assertEquals(expected.size, actual.size)
        expected.forEachIndexed { key, inntekt ->
            assertEquals(inntekt, actual[key])
        }
    }

    @Test
    fun `skal fjerne inntekter med utbetalingsperiode utenfor fom og tom`() {
        val aktørId = AktørId("11987654321")
        val tom = YearMonth.now()
        val fom = tom.minusMonths(1)

        val expected = listOf(
                Inntekt.Lønn(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(1500))
        )

        val inntekter = HentInntektListeBolkResponse().apply {
            with(arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    with(arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with(inntektListe) {
                                    add(Loennsinntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        utbetaltIPeriode = fom.minusMonths(1).toXmlGregorianCalendar()
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "889640782"
                                        }
                                        beloep = BigDecimal.valueOf(2500)
                                    })
                                    add(Loennsinntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        utbetaltIPeriode = tom.plusMonths(1).toXmlGregorianCalendar()
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "889640782"
                                        }
                                        beloep = BigDecimal.valueOf(3500)
                                    })
                                    add(Loennsinntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        utbetaltIPeriode = fom.toXmlGregorianCalendar()
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "889640782"
                                        }
                                        beloep = BigDecimal.valueOf(1500)
                                    })
                                }
                            }
                        })
                    }
                })
            }
        }

        val actual = InntektMapper.mapToInntekt(aktørId, fom, tom, inntekter.arbeidsInntektIdentListe)

        assertEquals(expected.size, actual.size)
        expected.forEachIndexed { key, inntekt ->
            assertEquals(inntekt, actual[key])
        }
    }
}
