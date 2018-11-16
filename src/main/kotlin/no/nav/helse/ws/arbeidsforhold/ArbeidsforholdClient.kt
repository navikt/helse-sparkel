package no.nav.helse.ws.arbeidsforhold

import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest

class ArbeidsforholdClient(private val arbeidsforhold: ArbeidsforholdV3) {

    fun finnArbeidsforholdForFnr(fnr: Fødselsnummer): Collection<Arbeidsforhold> {
        val request = FinnArbeidsforholdPrArbeidstakerRequest()
                .apply { ident = NorskIdent().apply { ident = fnr.value } }
                .apply { arbeidsforholdIPeriode = null } // optional, håper at null betyr _alle_ arbeidsforhold
                .apply { rapportertSomRegelverk = Regelverker().apply { kodeverksRef = RegelverkerValues.ALLE.name } }


        return arbeidsforhold.finnArbeidsforholdPrArbeidstaker(request)!!.arbeidsforhold!!
    }
}

enum class RegelverkerValues {
    FOER_A_ORDNINGEN, A_ORDNINGEN, ALLE
}
