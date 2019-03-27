package no.nav.helse.ws.inntekt

import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.inntekt.domain.Opptjeningsperiode
import no.nav.helse.ws.inntekt.domain.Virksomhet
import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
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
                no.nav.helse.ws.inntekt.domain.Inntekt(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(1500))
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
    fun `skal fjerne inntekter med utbetalingsperiode utenfor fom og tom`() {
        val aktørId = AktørId("11987654321")
        val tom = YearMonth.now()
        val fom = tom.minusMonths(1)

        val expected = listOf(
                no.nav.helse.ws.inntekt.domain.Inntekt(Virksomhet.Organisasjon(Organisasjonsnummer("889640782")),
                        fom, BigDecimal.valueOf(1500))
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
                                        utbetaltIPeriode = fom.minusMonths(1).toXmlGregorianCalendar()
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "889640782"
                                        }
                                        beloep = BigDecimal.valueOf(2500)
                                    })
                                    add(Inntekt().apply {
                                        inntektsmottaker = AktoerId().apply {
                                            aktoerId = aktørId.aktor
                                        }
                                        utbetaltIPeriode = tom.plusMonths(1).toXmlGregorianCalendar()
                                        virksomhet = Organisasjon().apply {
                                            orgnummer = "889640782"
                                        }
                                        beloep = BigDecimal.valueOf(3500)
                                    })
                                    add(Inntekt().apply {
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
