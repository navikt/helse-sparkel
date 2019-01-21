package no.nav.helse.ws.arbeidsforhold

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Failure
import no.nav.helse.Success
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.*
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkRequest
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArbeidsforholdClientTest {

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
                arbeidsavtale.add(Arbeidsavtale().apply {
                    fomGyldighetsperiode = LocalDate.of(2017, 1, 1).toXmlGregorianCalendar()
                    tomGyldighetsperiode = LocalDate.of(2019, 1, 1).toXmlGregorianCalendar()
                })
            }
        }

        val arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3)
        val result = arbeidsforholdClient.finnArbeidsforhold(Fødselsnummer("08078422069"), LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))

        when(result) {
            is Success<*> -> {
                val arbeidsforhold = result.data as List<Arbeidsforhold>

                Assertions.assertEquals(2, arbeidsforhold.size)

                Assertions.assertEquals(1234, arbeidsforhold[0].arbeidsforholdIDnav)
                Assertions.assertEquals(2, arbeidsforhold[0].arbeidsavtale.size)

                Assertions.assertEquals(5678, arbeidsforhold[1].arbeidsforholdIDnav)
                Assertions.assertEquals(1, arbeidsforhold[1].arbeidsavtale.size)
            }
            is Failure -> Assertions.fail("was not expecting a Failure: ${result.errors}")
        }
    }
}