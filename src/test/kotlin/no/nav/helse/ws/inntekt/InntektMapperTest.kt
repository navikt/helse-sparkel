package no.nav.helse.ws.inntekt

import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.AktoerId
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektIdent
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektInformasjon
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektMaaned
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Periode
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.PersonIdent
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
                no.nav.helse.ws.inntekt.Inntekt(Arbeidsgiver.Organisasjon("5678910"),
                        Opptjeningsperiode(fom.atDay(1), fom.atEndOfMonth(), false), BigDecimal.valueOf(1500))
        )

        val inntekter = HentInntektListeBolkResponse().apply {
            with(arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    with(arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with(inntektListe) {
                                    add(Inntekt().apply {
                                        inntektsmottaker = PersonIdent().apply {
                                            personIdent = "12345678911"
                                        }
                                    })
                                    add(Inntekt().apply {
                                        inntektsmottaker = Organisasjon().apply {
                                            orgnummer = "11223344"
                                        }
                                    })
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.atDay(1).toXmlGregorianCalendar()
                                            sluttDato = fom.atEndOfMonth().toXmlGregorianCalendar()
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "5678910"
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
    fun `skal fjerne inntekter med opptjeningsdato utenfor fom og tom`() {
        val aktørId = AktørId("11987654321")
        val tom = YearMonth.now()
        val fom = tom.minusMonths(1)

        val expected = listOf(
                no.nav.helse.ws.inntekt.Inntekt(Arbeidsgiver.Organisasjon("5678910"),
                        Opptjeningsperiode(fom.atDay(1), fom.atEndOfMonth(), false), BigDecimal.valueOf(1500))
        )

        val inntekter = HentInntektListeBolkResponse().apply {
            with(arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    with(arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with(inntektListe) {
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.minusMonths(1).atDay(1).toXmlGregorianCalendar()
                                            sluttDato = fom.minusMonths(1).atEndOfMonth().toXmlGregorianCalendar()
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "5678910"
                                        }
                                        beloep = BigDecimal.valueOf(2500)
                                    })
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = tom.plusMonths(1).atDay(1).toXmlGregorianCalendar()
                                            sluttDato = tom.plusMonths(1).atEndOfMonth().toXmlGregorianCalendar()
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "5678910"
                                        }
                                        beloep = BigDecimal.valueOf(3500)
                                    })
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.atDay(1).toXmlGregorianCalendar()
                                            sluttDato = fom.atEndOfMonth().toXmlGregorianCalendar()
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "5678910"
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
    fun `skal fjerne andre arbeidsgivere enn virksomheter`() {
        val aktørId = AktørId("11987654321")
        val tom = YearMonth.now()
        val fom = tom.minusMonths(1)

        val expected = listOf(
                no.nav.helse.ws.inntekt.Inntekt(Arbeidsgiver.Organisasjon("5678910"),
                        Opptjeningsperiode(fom.atDay(1), fom.atEndOfMonth(), false), BigDecimal.valueOf(1500))
        )

        val inntekter = HentInntektListeBolkResponse().apply {
            with(arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    with(arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with(inntektListe) {
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.atDay(1).toXmlGregorianCalendar()
                                            sluttDato = fom.atEndOfMonth().toXmlGregorianCalendar()
                                        }
                                        virksomhet = PersonIdent().apply {
                                            personIdent = "12345678911"
                                        }
                                        beloep = BigDecimal.valueOf(3500)
                                    })
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.atDay(1).toXmlGregorianCalendar()
                                            sluttDato = fom.atEndOfMonth().toXmlGregorianCalendar()
                                        }
                                        virksomhet = AktoerId().apply {
                                            aktoerId = "11223344"
                                        }
                                        beloep = BigDecimal.valueOf(2500)
                                    })
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.atDay(1).toXmlGregorianCalendar()
                                            sluttDato = fom.atEndOfMonth().toXmlGregorianCalendar()
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "5678910"
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
    fun `skal ta bort inntekter som ikke har opptjeningsperiode`() {
        val aktørId = AktørId("11987654321")
        val tom = YearMonth.now()
        val fom = tom.minusMonths(1)

        val expected = listOf(
                no.nav.helse.ws.inntekt.Inntekt(Arbeidsgiver.Organisasjon("5678910"),
                        Opptjeningsperiode(fom.atDay(1), fom.atEndOfMonth(), false), BigDecimal.valueOf(3000))
        )

        val inntekter = HentInntektListeBolkResponse().apply {
            with(arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    with(arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with(inntektListe) {
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "5678910"
                                        }
                                        utbetaltIPeriode = fom.minusMonths(1).toXmlGregorianCalendar()
                                        beloep = BigDecimal.valueOf(1500)
                                    })
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "5678910"
                                        }
                                        utbetaltIPeriode = fom.minusMonths(1).toXmlGregorianCalendar()
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.minusMonths(1).atDay(1).toXmlGregorianCalendar()
                                            sluttDato = fom.minusMonths(1).atEndOfMonth().toXmlGregorianCalendar()
                                        }
                                        beloep = BigDecimal.valueOf(2000)
                                    })
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.atDay(1).toXmlGregorianCalendar()
                                            sluttDato = fom.atEndOfMonth().toXmlGregorianCalendar()
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "5678910"
                                        }
                                        beloep = BigDecimal.valueOf(3000)
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
    fun `skal beholde inntekter som ikke har opptjeningsperiode, men hvor utbetaltIPeriode er innenfor`() {
        val aktørId = AktørId("11987654321")
        val tom = YearMonth.now()
        val fom = tom.minusMonths(1)

        val expected = listOf(
                no.nav.helse.ws.inntekt.Inntekt(Arbeidsgiver.Organisasjon("5678910"),
                        Opptjeningsperiode(fom.atDay(1), fom.atEndOfMonth(), true), BigDecimal.valueOf(3000))
        )

        val inntekter = HentInntektListeBolkResponse().apply {
            with(arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    with(arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with(inntektListe) {
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        utbetaltIPeriode = tom.plusMonths(1).toXmlGregorianCalendar()
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "5678910"
                                        }
                                        beloep = BigDecimal.valueOf(1500)
                                    })
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        utbetaltIPeriode = fom.toXmlGregorianCalendar()
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "5678910"
                                        }
                                        beloep = BigDecimal.valueOf(3000)
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
    fun `skal ta bort inntekter med ugyldig opptjeningsperiode`() {
        val aktørId = AktørId("11987654321")
        val tom = YearMonth.now()
        val fom = tom.minusMonths(1)

        val expected = listOf(
                no.nav.helse.ws.inntekt.Inntekt(Arbeidsgiver.Organisasjon("5678910"),
                        Opptjeningsperiode(fom.atDay(1), fom.atEndOfMonth(), false), BigDecimal.valueOf(1500))
        )

        val inntekter = HentInntektListeBolkResponse().apply {
            with(arbeidsInntektIdentListe) {
                add(ArbeidsInntektIdent().apply {
                    with(arbeidsInntektMaaned) {
                        add(ArbeidsInntektMaaned().apply {
                            arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                                with(inntektListe) {
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.atEndOfMonth().toXmlGregorianCalendar()
                                            sluttDato = fom.atDay(1).toXmlGregorianCalendar()
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "5678910"
                                        }
                                        beloep = BigDecimal.valueOf(1500)
                                    })
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.atDay(1).toXmlGregorianCalendar()
                                            sluttDato = fom.plusMonths(1).atDay(1).toXmlGregorianCalendar()
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "5678910"
                                        }
                                        beloep = BigDecimal.valueOf(1500)
                                    })
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        opptjeningsperiode = Periode().apply {
                                            startDato = fom.atDay(1).toXmlGregorianCalendar()
                                            sluttDato = fom.atEndOfMonth().toXmlGregorianCalendar()
                                        }
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "5678910"
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
