package no.nav.helse.ws.arbeidsforhold

import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.common.toLocalDate
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
import org.slf4j.LoggerFactory
import java.time.LocalDate

fun dateOverlap(range1Start: LocalDate, range1End: LocalDate, range2Start: LocalDate, range2End: LocalDate): Boolean {
    val isValidInterval = range1End >= range1Start && range2End >= range2Start
    val isOverlap = range1Start <= range2End && range1End >= range2Start
    return isValidInterval && isOverlap
}

class ArbeidsforholdClient(private val arbeidsforholdV3: ArbeidsforholdV3) {

    private val log = LoggerFactory.getLogger("ArbeidsforholdClient")

    fun finnArbeidsforhold(fnr: Fødselsnummer, fom: LocalDate, tom: LocalDate): OppslagResult {
        when(val result = finnArbeidsforholdForFnr(fnr, fom, tom)) {
            is Success<*> -> {
                return Success((result.data as FinnArbeidsforholdPrArbeidstakerResponse).arbeidsforhold.onEach {
                    when (val historikkResponse = finnHistorikkForArbeidsforhold(it, fom, tom)) {
                        is Success<*> -> {
                            it.arbeidsavtale.clear()
                            it.arbeidsavtale.addAll(historikkResponse.data as List<Arbeidsavtale>)
                        }
                        is Failure -> {
                            log.error("Feil ved henting av historikk: ${historikkResponse.errors}")
                        }
                    }
                })
            }
        }
        return Failure(listOf("oh no"))
    }

    private fun finnArbeidsforholdForFnr(fnr: Fødselsnummer, fom: LocalDate, tom: LocalDate): OppslagResult {
        val request = FinnArbeidsforholdPrArbeidstakerRequest()
                .apply {
                    ident = NorskIdent().apply { ident = fnr.value }
                    arbeidsforholdIPeriode = Periode().apply {
                        this.fom = fom.toXmlGregorianCalendar()
                        this.tom = tom.toXmlGregorianCalendar()
                    }
                    rapportertSomRegelverk = Regelverker().apply {
                        value = RegelverkerValues.A_ORDNINGEN.name
                        kodeRef = RegelverkerValues.A_ORDNINGEN.name
                    }
                }
        return try {
            Success(arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(request))
        } catch (ex: Exception) {
            log.error("Error while doing arbeidsforhold lookup", ex)
            Failure(listOf(ex.message ?: "unknown error"))
        }
    }

    private fun finnHistorikkForArbeidsforhold(arbeidsforhold: Arbeidsforhold, fom: LocalDate, tom: LocalDate): OppslagResult {
        val request = HentArbeidsforholdHistorikkRequest().apply {
            arbeidsforholdId = arbeidsforhold.arbeidsforholdIDnav
        }
        return try {
            Success(arbeidsforholdV3.hentArbeidsforholdHistorikk(request).arbeidsforhold.arbeidsavtale.filter {
                dateOverlap(it.fomGyldighetsperiode.toLocalDate(), it.tomGyldighetsperiode?.toLocalDate() ?: LocalDate.MAX, fom, tom)
            })
        } catch (err: Exception) {
            Failure(listOf(err.message ?: "unknown error"))
        }
    }
}

enum class RegelverkerValues {
    FOER_A_ORDNINGEN, A_ORDNINGEN, ALLE
}
