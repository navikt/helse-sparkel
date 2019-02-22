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
        val actual = arbeidsforholdClient.finnArbeidsforhold(AktørId("08078422069"), LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))

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
    fun `skal returnere liste over arbeidsforhold arbeidsforhold`() {
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
}
