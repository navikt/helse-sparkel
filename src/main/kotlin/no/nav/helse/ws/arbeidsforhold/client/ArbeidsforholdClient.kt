package no.nav.helse.ws.arbeidsforhold.client

import no.nav.helse.Either
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Periode
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkRequest
import org.slf4j.LoggerFactory
import java.time.LocalDate

class ArbeidsforholdClient(private val arbeidsforholdV3: ArbeidsforholdV3) {

    companion object {
        private val log = LoggerFactory.getLogger(ArbeidsforholdClient::class.java)
    }

    fun finnArbeidsforhold(aktørId: AktørId, fom: LocalDate, tom: LocalDate): Either<Exception, List<Arbeidsforhold>> {
        val request = FinnArbeidsforholdPrArbeidstakerRequest()
                .apply {
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
        return try {
            Either.Right(arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(request).arbeidsforhold.toList())
        } catch (ex: Exception) {
            log.error("Error while doing arbeidsforhold lookup", ex)
            Either.Left(ex)
        }
    }

    fun finnHistoriskeArbeidsavtaler(arbeidsforholdIDnav: Long) =
        HentArbeidsforholdHistorikkRequest().apply {
            arbeidsforholdId = arbeidsforholdIDnav
        }.let { request ->
            try {
                Either.Right(arbeidsforholdV3.hentArbeidsforholdHistorikk(request).arbeidsforhold.arbeidsavtale)
            } catch (ex: Exception) {
                log.error("Error while doing arbeidsforhold historikk lookup", ex)
                Either.Left(ex)
            }
        }
}
