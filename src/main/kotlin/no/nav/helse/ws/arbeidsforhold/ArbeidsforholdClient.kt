package no.nav.helse.ws.arbeidsforhold

import io.prometheus.client.Counter
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.WSFinnArbeidsforholdPrArbeidstakerRequest
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.WSNorskIdent
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.WSRegelverker
import org.slf4j.LoggerFactory

class ArbeidsforholdClient(private val arbeidsforhold: ArbeidsforholdV3) {

    private val log = LoggerFactory.getLogger("ArbeidsforholdClient")

    private val counter = Counter.build()
            .name("oppslag_arbeidsforhold")
            .labelNames("status")
            .help("Antall registeroppslag av arbeidsforhold for person")
            .register()

    fun finnArbeidsforholdForFnr(fnr: Fødselsnummer): OppslagResult {
        val request = WSFinnArbeidsforholdPrArbeidstakerRequest()
                .apply { ident = WSNorskIdent().apply { ident = fnr.value } }
                .apply { arbeidsforholdIPeriode = null } // optional, håper at null betyr _alle_ arbeidsforhold
                .apply { rapportertSomRegelverk = WSRegelverker().apply { kodeverksRef = RegelverkerValues.ALLE.name } }

        return try {
            val remoteResult = arbeidsforhold.finnArbeidsforholdPrArbeidstaker(request)
            counter.labels("success").inc()
            Success(remoteResult)
        } catch (ex: Exception) {
            log.error("Error while doing arbeidsforhold lookup", ex)
            counter.labels("failure").inc()
            Failure(listOf(ex.message ?: "unknown error"))
        }
    }
}

enum class RegelverkerValues {
    FOER_A_ORDNINGEN, A_ORDNINGEN, ALLE
}
