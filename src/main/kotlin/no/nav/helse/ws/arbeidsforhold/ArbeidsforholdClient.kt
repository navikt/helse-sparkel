package no.nav.helse.ws.arbeidsforhold

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.*
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkRequest
import org.slf4j.LoggerFactory
import java.time.LocalDate
import javax.xml.datatype.XMLGregorianCalendar
import java.time.ZoneId
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory


fun LocalDate.toXmlGregorianCalendar() : XMLGregorianCalendar {
    val gcal = GregorianCalendar.from(this.atStartOfDay(ZoneId.systemDefault()))
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
}

private fun XMLGregorianCalendar.toLocalDate(): LocalDate {
    return this.toGregorianCalendar().toZonedDateTime().toLocalDate()
}

fun dateOverlap(range1Start: LocalDate, range1End: LocalDate, range2Start: LocalDate, range2End: LocalDate): Boolean {
    val isValidInterval = range1End >= range1Start && range2End >= range2Start
    val isOverlap = range1Start <= range2End && range1End >= range2Start
    return isValidInterval && isOverlap
}

class ArbeidsforholdClient(private val arbeidsforholdV3: ArbeidsforholdV3) {

    private val log = LoggerFactory.getLogger("ArbeidsforholdClient")

    private val counter = Counter.build()
            .name("oppslag_arbeidsforhold")
            .labelNames("status")
            .help("Antall registeroppslag av arbeidsforhold for person")
            .register()

    private val finnArbeidsforholdPrArbeidstakerTimer = Histogram.build()
            .name("finn_arbeidsforhold_pr_arbeidstaker_seconds")
            .help("latency for ArbeidsforholdV3.finnArbeidsforholdPrArbeidstaker()").register()

    private val hentArbeidsforholdHistorikkTimer = Histogram.build()
            .name("hent_arbeidsforhold_historikk_seconds")
            .help("latency for ArbeidsforholdV3.hentArbeidsforholdHistorikk()").register()

    fun finnArbeidsforhold(fnr: Fødselsnummer, fom: LocalDate, tom: LocalDate): OppslagResult {
        when(val result = finnArbeidsforholdForFnr(fnr, fom, tom)) {
            is Success<*> -> {
                return Success((result.data as FinnArbeidsforholdPrArbeidstakerResponse).arbeidsforhold.onEach {
                    when (val historikkResponse = finnHistorikkForArbeidsforhold(it, fom, tom)) {
                        is Success<*> -> {
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
        return finnArbeidsforholdPrArbeidstakerTimer.time<OppslagResult> {
            try {
                val remoteResult = arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(request)
                counter.labels("success").inc()
                Success(remoteResult)
            } catch (ex: Exception) {
                log.error("Error while doing arbeidsforhold lookup", ex)
                counter.labels("failure").inc()
                Failure(listOf(ex.message ?: "unknown error"))
            }
        }
    }

    private fun finnHistorikkForArbeidsforhold(arbeidsforhold: Arbeidsforhold, fom: LocalDate, tom: LocalDate): OppslagResult {
        val request = HentArbeidsforholdHistorikkRequest().apply {
            arbeidsforholdId = arbeidsforhold.arbeidsforholdIDnav
        }
        return hentArbeidsforholdHistorikkTimer.time<OppslagResult> {
            try {
                Success(arbeidsforholdV3.hentArbeidsforholdHistorikk(request).arbeidsforhold.arbeidsavtale.filter {
                    dateOverlap(it.fomGyldighetsperiode.toLocalDate(), it.tomGyldighetsperiode?.toLocalDate() ?: LocalDate.MAX, fom, tom)
                })
            } catch (err: Exception) {
                Failure(listOf(err.message ?: "unknown error"))
            }
        }
    }
}

enum class RegelverkerValues {
    FOER_A_ORDNINGEN, A_ORDNINGEN, ALLE
}
