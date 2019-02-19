package no.nav.helse.ws.arbeidsforhold

import io.ktor.http.HttpStatusCode
import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.common.toLocalDate
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.flatMap
import no.nav.helse.map
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
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

    fun finnArbeidsforhold(fnr: Fødselsnummer, fom: LocalDate, tom: LocalDate): OppslagResult<Feil, List<Arbeidsforhold>> {
        return finnArbeidsforholdForFnr(fnr, fom, tom).map { arbeidsforholdResponse ->
            arbeidsforholdResponse.arbeidsforhold
        }.flatMap { listeOverArbeidsforhold ->
            listeOverArbeidsforhold.map { arbeidsforhold ->
                finnHistorikkForArbeidsforhold(arbeidsforhold, fom, tom)
            }.let { listeOverArbeidsforholdMedHistorikk ->
                OppslagResult.Ok(listeOverArbeidsforholdMedHistorikk.map { historikkResult ->
                    when (historikkResult) {
                        is OppslagResult.Ok -> historikkResult.data
                        is OppslagResult.Feil -> {
                            return@let historikkResult.copy()
                        }
                    }
                })
            }
        }
    }

    private fun finnArbeidsforholdForFnr(fnr: Fødselsnummer, fom: LocalDate, tom: LocalDate): OppslagResult<Feil, FinnArbeidsforholdPrArbeidstakerResponse> {
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
            OppslagResult.Ok(arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(request))
        } catch (ex: Exception) {
            log.error("Error while doing arbeidsforhold lookup", ex)
            OppslagResult.Feil(HttpStatusCode.InternalServerError, Feil.Exception(ex.message ?: "unknown error", ex))
        }
    }

    private fun finnHistorikkForArbeidsforhold(arbeidsforhold: Arbeidsforhold, fom: LocalDate, tom: LocalDate): OppslagResult<Feil, Arbeidsforhold> {
        val request = HentArbeidsforholdHistorikkRequest().apply {
            arbeidsforholdId = arbeidsforhold.arbeidsforholdIDnav
        }
        return try {
            val arbeidsforhold = arbeidsforholdV3.hentArbeidsforholdHistorikk(request).arbeidsforhold

            val arbeidsforholdMedFiltrertHistorikk = arbeidsforhold.also {
                val filtrerteArbeidsavtaler = it.arbeidsavtale.filter { arbeidsavtale ->
                    dateOverlap(arbeidsavtale.fomGyldighetsperiode.toLocalDate(), arbeidsavtale.tomGyldighetsperiode?.toLocalDate() ?: LocalDate.MAX, fom, tom)
                }
                it.arbeidsavtale.clear()
                it.arbeidsavtale.addAll(filtrerteArbeidsavtaler)
            }

            OppslagResult.Ok(arbeidsforholdMedFiltrertHistorikk)
        } catch (err: Exception) {
            OppslagResult.Feil(HttpStatusCode.InternalServerError, Feil.Exception(err.message ?: "unknown error", err))
        }
    }
}

enum class RegelverkerValues {
    FOER_A_ORDNINGEN, A_ORDNINGEN, ALLE
}
