package no.nav.helse.ws.arbeidsforhold

import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsavtale
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Periode
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkRequest
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate

class ArbeidsforholdClientTest {

    @Test
    fun `skal returnere feil når arbeidsforholdoppslag gir feil`() {
        val arbeidsforholdV3 = mockk<ArbeidsforholdV3>()

        every {
            arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(any())
        } throws(Exception("SOAP fault"))

        val arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3)
        val actual = arbeidsforholdClient.finnArbeidsforholdMedHistorikkOverArbeidsavtaler(AktørId("08078422069"), LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))

        when (actual) {
            is OppslagResult.Feil -> {
                when (actual.feil) {
                    is Feil.Exception -> {
                        Assertions.assertEquals(HttpStatusCode.InternalServerError, actual.httpCode)
                        Assertions.assertEquals("SOAP fault", (actual.feil as Feil.Exception).feilmelding)
                    }
                    else -> fail { "Expected Feil.Exception to be returned" }
                }
            }
            else -> fail { "Expected OppslagResult.Feil to be returned" }
        }
    }

    @Test
    fun `skal returnere feil når historikkoppslag gir feil`() {
        val arbeidsforholdV3 = mockk<ArbeidsforholdV3>()

        every {
            arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(any())
        } returns FinnArbeidsforholdPrArbeidstakerResponse().apply {
            arbeidsforhold.add(Arbeidsforhold().apply {
                arbeidsforholdIDnav = 1234
            })
        }

        every {
            arbeidsforholdV3.hentArbeidsforholdHistorikk(any())
        } throws(Exception("SOAP fault"))

        val arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3)
        val actual = arbeidsforholdClient.finnArbeidsforholdMedHistorikkOverArbeidsavtaler(AktørId("08078422069"), LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))

        when (actual) {
            is OppslagResult.Feil -> {
                when (actual.feil) {
                    is Feil.Exception -> {
                        Assertions.assertEquals(HttpStatusCode.InternalServerError, actual.httpCode)
                        Assertions.assertEquals("SOAP fault", (actual.feil as Feil.Exception).feilmelding)
                    }
                    else -> fail { "Expected Feil.Exception to be returned" }
                }
            }
            else -> fail { "Expected OppslagResult.Feil to be returned" }
        }
    }

    @Test
    fun `should fetch historikk for arbeidsforhold`() {
        val arbeidsforholdV3 = mockk<ArbeidsforholdV3>()
        every {
            val request = FinnArbeidsforholdPrArbeidstakerRequest()
                    .apply {
                        ident = NorskIdent().apply { ident = "08078422069" }
                        arbeidsforholdIPeriode = Periode().apply {
                            this.fom = LocalDate.of(2018, 1, 1).toXmlGregorianCalendar()
                            this.tom = LocalDate.of(2018, 12, 1).toXmlGregorianCalendar()
                        }
                        rapportertSomRegelverk = Regelverker().apply {
                            value = "A_ORDNINGEN"
                            kodeRef = "A_ORDNINGEN"
                        }
                    }
            arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(match {
                it.ident.ident == request.ident.ident &&
                        it.arbeidsforholdIPeriode.fom == request.arbeidsforholdIPeriode.fom &&
                        it.arbeidsforholdIPeriode.tom == request.arbeidsforholdIPeriode.tom &&
                        it.rapportertSomRegelverk.value == request.rapportertSomRegelverk.value &&
                        it.rapportertSomRegelverk.kodeverksRef == request.rapportertSomRegelverk.kodeverksRef
            })
        } returns FinnArbeidsforholdPrArbeidstakerResponse().apply {
            arbeidsforhold.add(Arbeidsforhold().apply {
                arbeidsforholdIDnav = 1234
            })
            arbeidsforhold.add(Arbeidsforhold().apply {
                arbeidsforholdIDnav = 5678
            })
        }

        every {
            val request = HentArbeidsforholdHistorikkRequest().apply {
                arbeidsforholdId = 1234
            }
            arbeidsforholdV3.hentArbeidsforholdHistorikk(match {
                it.arbeidsforholdId == request.arbeidsforholdId
            })
        } returns HentArbeidsforholdHistorikkResponse().apply {
            arbeidsforhold = Arbeidsforhold().apply {
                arbeidsforholdIDnav = 1234
                arbeidsavtale.add(Arbeidsavtale().apply {
                    fomGyldighetsperiode = LocalDate.of(2018, 1, 2).toXmlGregorianCalendar()
                    tomGyldighetsperiode = LocalDate.of(2018, 6, 1).toXmlGregorianCalendar()
                })
                arbeidsavtale.add(Arbeidsavtale().apply {
                    fomGyldighetsperiode = LocalDate.of(2017, 1, 1).toXmlGregorianCalendar()
                    tomGyldighetsperiode = LocalDate.of(2018, 1, 1).toXmlGregorianCalendar()
                })
            }
        }

        every {
            val request = HentArbeidsforholdHistorikkRequest().apply {
                arbeidsforholdId = 5678
            }
            arbeidsforholdV3.hentArbeidsforholdHistorikk(match {
                it.arbeidsforholdId == request.arbeidsforholdId
            })
        } returns HentArbeidsforholdHistorikkResponse().apply {
            arbeidsforhold = Arbeidsforhold().apply {
                arbeidsforholdIDnav = 5678
                arbeidsavtale.add(Arbeidsavtale().apply {
                    fomGyldighetsperiode = LocalDate.of(2017, 1, 1).toXmlGregorianCalendar()
                    tomGyldighetsperiode = LocalDate.of(2019, 1, 1).toXmlGregorianCalendar()
                })
            }
        }

        val arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3)
        val result = arbeidsforholdClient.finnArbeidsforholdMedHistorikkOverArbeidsavtaler(AktørId("08078422069"), LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))

        when(result) {
            is OppslagResult.Ok -> {
                val arbeidsforhold = result.data

                Assertions.assertEquals(2, arbeidsforhold.size)

                Assertions.assertEquals(1234, arbeidsforhold[0].arbeidsforholdIDnav)
                Assertions.assertEquals(2, arbeidsforhold[0].arbeidsavtale.size)

                Assertions.assertEquals(5678, arbeidsforhold[1].arbeidsforholdIDnav)
                Assertions.assertEquals(1, arbeidsforhold[1].arbeidsavtale.size)
            }
            is OppslagResult.Feil -> Assertions.fail("was not expecting a Failure: ${result.feil}")
        }
    }

    @Test
    fun `should handle arbeidsavtaler with no tomGyldighetsperiode set`() {
        val arbeidsforholdV3 = mockk<ArbeidsforholdV3>()
        every {
            val request = FinnArbeidsforholdPrArbeidstakerRequest()
                    .apply {
                        ident = NorskIdent().apply { ident = "08078422069" }
                        arbeidsforholdIPeriode = Periode().apply {
                            this.fom = LocalDate.of(2018, 1, 1).toXmlGregorianCalendar()
                            this.tom = LocalDate.of(2018, 12, 1).toXmlGregorianCalendar()
                        }
                        rapportertSomRegelverk = Regelverker().apply {
                            value = "A_ORDNINGEN"
                            kodeRef = "A_ORDNINGEN"
                        }
                    }
            arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(match {
                it.ident.ident == request.ident.ident &&
                        it.arbeidsforholdIPeriode.fom == request.arbeidsforholdIPeriode.fom &&
                        it.arbeidsforholdIPeriode.tom == request.arbeidsforholdIPeriode.tom &&
                        it.rapportertSomRegelverk.value == request.rapportertSomRegelverk.value &&
                        it.rapportertSomRegelverk.kodeverksRef == request.rapportertSomRegelverk.kodeverksRef
            })
        } returns FinnArbeidsforholdPrArbeidstakerResponse().apply {
            arbeidsforhold.add(Arbeidsforhold().apply {
                arbeidsforholdIDnav = 1234
            })
            arbeidsforhold.add(Arbeidsforhold().apply {
                arbeidsforholdIDnav = 5678
            })
        }

        every {
            val request = HentArbeidsforholdHistorikkRequest().apply {
                arbeidsforholdId = 1234
            }
            arbeidsforholdV3.hentArbeidsforholdHistorikk(match {
                it.arbeidsforholdId == request.arbeidsforholdId
            })
        } returns HentArbeidsforholdHistorikkResponse().apply {
            arbeidsforhold = Arbeidsforhold().apply {
                arbeidsforholdIDnav = 1234
                arbeidsavtale.add(Arbeidsavtale().apply {
                    fomGyldighetsperiode = LocalDate.of(2018, 1, 2).toXmlGregorianCalendar()
                    tomGyldighetsperiode = null
                })
                arbeidsavtale.add(Arbeidsavtale().apply {
                    fomGyldighetsperiode = LocalDate.of(2017, 1, 1).toXmlGregorianCalendar()
                    tomGyldighetsperiode = null
                })
            }
        }

        every {
            val request = HentArbeidsforholdHistorikkRequest().apply {
                arbeidsforholdId = 5678
            }
            arbeidsforholdV3.hentArbeidsforholdHistorikk(match {
                it.arbeidsforholdId == request.arbeidsforholdId
            })
        } returns HentArbeidsforholdHistorikkResponse().apply {
            arbeidsforhold = Arbeidsforhold().apply {
                arbeidsforholdIDnav = 5678
                arbeidsavtale.add(Arbeidsavtale().apply {
                    fomGyldighetsperiode = LocalDate.of(2017, 1, 1).toXmlGregorianCalendar()
                    tomGyldighetsperiode = null
                })
            }
        }

        val arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3)
        val result = arbeidsforholdClient.finnArbeidsforholdMedHistorikkOverArbeidsavtaler(AktørId("08078422069"), LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))

        when(result) {
            is OppslagResult.Ok -> {
                val arbeidsforhold = result.data

                Assertions.assertEquals(2, arbeidsforhold.size)

                Assertions.assertEquals(1234, arbeidsforhold[0].arbeidsforholdIDnav)
                Assertions.assertEquals(2, arbeidsforhold[0].arbeidsavtale.size)

                Assertions.assertEquals(5678, arbeidsforhold[1].arbeidsforholdIDnav)
                Assertions.assertEquals(1, arbeidsforhold[1].arbeidsavtale.size)
            }
            is OppslagResult.Feil -> Assertions.fail("was not expecting a Failure: ${result.feil}")
        }
    }

    @Test
    fun `should not include arbeidsavtaler outside interval`() {
        val arbeidsforholdV3 = mockk<ArbeidsforholdV3>()
        every {
            val request = FinnArbeidsforholdPrArbeidstakerRequest()
                    .apply {
                        ident = NorskIdent().apply { ident = "08078422069" }
                        arbeidsforholdIPeriode = Periode().apply {
                            this.fom = LocalDate.of(2018, 1, 1).toXmlGregorianCalendar()
                            this.tom = LocalDate.of(2018, 12, 1).toXmlGregorianCalendar()
                        }
                        rapportertSomRegelverk = Regelverker().apply {
                            value = "A_ORDNINGEN"
                            kodeRef = "A_ORDNINGEN"
                        }
                    }
            arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(match {
                it.ident.ident == request.ident.ident &&
                        it.arbeidsforholdIPeriode.fom == request.arbeidsforholdIPeriode.fom &&
                        it.arbeidsforholdIPeriode.tom == request.arbeidsforholdIPeriode.tom &&
                        it.rapportertSomRegelverk.value == request.rapportertSomRegelverk.value &&
                        it.rapportertSomRegelverk.kodeverksRef == request.rapportertSomRegelverk.kodeverksRef
            })
        } returns FinnArbeidsforholdPrArbeidstakerResponse().apply {
            arbeidsforhold.add(Arbeidsforhold().apply {
                arbeidsforholdIDnav = 1234
            })
            arbeidsforhold.add(Arbeidsforhold().apply {
                arbeidsforholdIDnav = 5678
            })
        }

        every {
            val request = HentArbeidsforholdHistorikkRequest().apply {
                arbeidsforholdId = 1234
            }
            arbeidsforholdV3.hentArbeidsforholdHistorikk(match {
                it.arbeidsforholdId == request.arbeidsforholdId
            })
        } returns HentArbeidsforholdHistorikkResponse().apply {
            arbeidsforhold = Arbeidsforhold().apply {
                arbeidsforholdIDnav = 1234
                arbeidsavtale.add(Arbeidsavtale().apply {
                    fomGyldighetsperiode = LocalDate.of(2018, 1, 2).toXmlGregorianCalendar()
                    tomGyldighetsperiode = null
                })
                arbeidsavtale.add(Arbeidsavtale().apply {
                    fomGyldighetsperiode = LocalDate.of(2015, 1, 1).toXmlGregorianCalendar()
                    tomGyldighetsperiode = LocalDate.of(2016, 12, 31).toXmlGregorianCalendar()
                })
            }
        }

        every {
            val request = HentArbeidsforholdHistorikkRequest().apply {
                arbeidsforholdId = 5678
            }
            arbeidsforholdV3.hentArbeidsforholdHistorikk(match {
                it.arbeidsforholdId == request.arbeidsforholdId
            })
        } returns HentArbeidsforholdHistorikkResponse().apply {
            arbeidsforhold = Arbeidsforhold().apply {
                arbeidsforholdIDnav = 5678
                arbeidsavtale.add(Arbeidsavtale().apply {
                    fomGyldighetsperiode = LocalDate.of(2017, 1, 1).toXmlGregorianCalendar()
                    tomGyldighetsperiode = null
                })
            }
        }

        val arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3)
        val result = arbeidsforholdClient.finnArbeidsforholdMedHistorikkOverArbeidsavtaler(AktørId("08078422069"), LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))

        when(result) {
            is OppslagResult.Ok -> {
                val arbeidsforhold = result.data

                Assertions.assertEquals(2, arbeidsforhold.size)

                Assertions.assertEquals(1234, arbeidsforhold[0].arbeidsforholdIDnav)
                Assertions.assertEquals(1, arbeidsforhold[0].arbeidsavtale.size)

                Assertions.assertEquals(5678, arbeidsforhold[1].arbeidsforholdIDnav)
                Assertions.assertEquals(1, arbeidsforhold[1].arbeidsavtale.size)
            }
            is OppslagResult.Feil -> Assertions.fail("was not expecting a Failure: ${result.feil}")
        }
    }
}
