package no.nav.helse.ws.arbeidsforhold

import io.mockk.every
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.helse.OppslagResult
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.Fødselsnummer
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArbeidsforholdClientTest {

    @BeforeEach
    fun `clear prometheus registry before test`() {
        CollectorRegistry.defaultRegistry.clear()
    }

    @AfterEach
    fun `clear prometheus registry after test`() {
        CollectorRegistry.defaultRegistry.clear()
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
        val result = arbeidsforholdClient.finnArbeidsforhold(Fødselsnummer("08078422069"), LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))

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
        val result = arbeidsforholdClient.finnArbeidsforhold(Fødselsnummer("08078422069"), LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))

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
        val result = arbeidsforholdClient.finnArbeidsforhold(Fødselsnummer("08078422069"), LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))

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
