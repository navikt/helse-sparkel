package no.nav.helse.ws.arbeidsforhold.client

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Either
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.*
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
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
        val actual = arbeidsforholdClient.finnArbeidsforhold(AktørId("08078422069"), LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))

        when (actual) {
            is Either.Left -> assertEquals("SOAP fault", actual.left.message)
            else -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal returnere feil når historikkoppslag gir feil`() {
        val arbeidsforholdV3 = mockk<ArbeidsforholdV3>()

        val arbeidsforholdIDnav = 12345678L

        every {
            arbeidsforholdV3.hentArbeidsforholdHistorikk(match { request ->
                request.arbeidsforholdId == arbeidsforholdIDnav
            })
        } throws(Exception("SOAP fault"))

        val arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3)
        val actual = arbeidsforholdClient.finnHistoriskeArbeidsavtaler(arbeidsforholdIDnav)

        when (actual) {
            is Either.Left -> assertEquals("SOAP fault", actual.left.message)
            else -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal hente historikk for arbeidsforhold`() {
        val arbeidsforholdV3 = mockk<ArbeidsforholdV3>()

        val arbeidsforholdId = 12345678L

        val arbeidsavtale1 = Arbeidsavtale().apply {
            fomGyldighetsperiode = LocalDate.of(2018, 1, 2).toXmlGregorianCalendar()
            tomGyldighetsperiode = LocalDate.of(2018, 6, 1).toXmlGregorianCalendar()
        }
        val arbeidsavtale2 = Arbeidsavtale().apply {
            fomGyldighetsperiode = LocalDate.of(2017, 1, 1).toXmlGregorianCalendar()
            tomGyldighetsperiode = LocalDate.of(2018, 1, 1).toXmlGregorianCalendar()
        }

        every {
            arbeidsforholdV3.hentArbeidsforholdHistorikk(match { request ->
                request.arbeidsforholdId == arbeidsforholdId
            })
        } returns HentArbeidsforholdHistorikkResponse().apply {
            arbeidsforhold = Arbeidsforhold().apply {
                arbeidsforholdIDnav = arbeidsforholdId
                arbeidsavtale.add(arbeidsavtale1)
                arbeidsavtale.add(arbeidsavtale2)
            }
        }

        val arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3)
        val result = arbeidsforholdClient.finnHistoriskeArbeidsavtaler(arbeidsforholdId)

        when(result) {
            is Either.Right -> {
                val avtaler = result.right

                assertEquals(2, avtaler.size)

                assertEquals(arbeidsavtale1.fomGyldighetsperiode, avtaler[0].fomGyldighetsperiode)
                assertEquals(arbeidsavtale1.tomGyldighetsperiode, avtaler[0].tomGyldighetsperiode)

                assertEquals(arbeidsavtale2.fomGyldighetsperiode, avtaler[1].fomGyldighetsperiode)
                assertEquals(arbeidsavtale2.tomGyldighetsperiode, avtaler[1].tomGyldighetsperiode)
            }
            else -> fail { "Expected Either.Right to be returned" }
        }
    }

    @Test
    fun `skal returnere liste over arbeidsforhold`() {
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
                with (arbeidsavtale) {
                    add(Arbeidsavtale())
                    add(Arbeidsavtale())
                }
            })
            arbeidsforhold.add(Arbeidsforhold().apply {
                arbeidsforholdIDnav = 5678
                with (arbeidsavtale) {
                    add(Arbeidsavtale())
                }
            })
        }

        val arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3)
        val result = arbeidsforholdClient.finnArbeidsforhold(AktørId("08078422069"), LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))

        when(result) {
            is Either.Right -> {
                val arbeidsforhold = result.right

                assertEquals(2, arbeidsforhold.size)

                assertEquals(1234, arbeidsforhold[0].arbeidsforholdIDnav)
                assertEquals(2, arbeidsforhold[0].arbeidsavtale.size)

                assertEquals(5678, arbeidsforhold[1].arbeidsforholdIDnav)
                assertEquals(1, arbeidsforhold[1].arbeidsavtale.size)
            }
            is Either.Left -> Assertions.fail("was not expecting a Failure: ${result.left}")
        }
    }
}
