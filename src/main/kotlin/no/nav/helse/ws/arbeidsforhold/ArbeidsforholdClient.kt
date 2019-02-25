package no.nav.helse.ws.arbeidsforhold

import no.nav.helse.OppslagResult
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.map
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Periode
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse
import org.slf4j.LoggerFactory
import java.time.LocalDate

class ArbeidsforholdClient(private val arbeidsforholdV3: ArbeidsforholdV3) {

    private val log = LoggerFactory.getLogger("ArbeidsforholdClient")

    fun finnArbeidsforhold(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            finnArbeidsforholdForFnr(aktørId, fom, tom).map { arbeidsforholdResponse ->
                arbeidsforholdResponse.arbeidsforhold!!
                        .map { arbeidsforhold ->
                            arbeidsforhold!!
                        }
            }.map {
                it.toList()
            }

    private fun finnArbeidsforholdForFnr(aktørId: AktørId, fom: LocalDate, tom: LocalDate): OppslagResult<Exception, FinnArbeidsforholdPrArbeidstakerResponse> {
        val request = FinnArbeidsforholdPrArbeidstakerRequest()
                .apply {
                    ident = NorskIdent().apply { ident = aktørId.aktor }
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
            OppslagResult.Feil(ex)
        }
    }
}

enum class RegelverkerValues {
    FOER_A_ORDNINGEN, A_ORDNINGEN, ALLE
}
