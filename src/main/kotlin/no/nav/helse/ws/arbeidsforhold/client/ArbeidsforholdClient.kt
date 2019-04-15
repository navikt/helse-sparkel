package no.nav.helse.ws.arbeidsforhold.client

import arrow.core.Try
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Periode
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkRequest
import java.time.LocalDate

class ArbeidsforholdClient(private val arbeidsforholdV3: ArbeidsforholdV3) {

    fun finnArbeidsforhold(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            Try {
                arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(hentArbeidsforholdRequest(aktørId, fom, tom)).arbeidsforhold.toList()
            }

    fun finnHistoriskeArbeidsavtaler(arbeidsforholdIDnav: Long) =
            Try {
                arbeidsforholdV3.hentArbeidsforholdHistorikk(hentArbeidsavtalerRequest(arbeidsforholdIDnav)).arbeidsforhold.arbeidsavtale.toList()
            }

    private fun hentArbeidsforholdRequest(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            FinnArbeidsforholdPrArbeidstakerRequest().apply {
                ident = NorskIdent().apply {
                    ident = aktørId.aktor
                }
                arbeidsforholdIPeriode = Periode().apply {
                    this.fom = fom.toXmlGregorianCalendar()
                    this.tom = tom.toXmlGregorianCalendar()
                }
                rapportertSomRegelverk = Regelverker().apply {
                    value = RegelverkerValues.A_ORDNINGEN.name
                    kodeRef = RegelverkerValues.A_ORDNINGEN.name
                }
            }

    private fun hentArbeidsavtalerRequest(arbeidsforholdIDnav: Long) =
            HentArbeidsforholdHistorikkRequest().apply {
                arbeidsforholdId = arbeidsforholdIDnav
            }
}
