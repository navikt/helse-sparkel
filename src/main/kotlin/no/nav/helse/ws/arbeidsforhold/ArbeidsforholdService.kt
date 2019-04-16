package no.nav.helse.ws.arbeidsforhold

import arrow.core.flatMap
import no.nav.helse.Feilårsak
import no.nav.helse.arrow.sequenceU
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.arbeidsforhold.client.ArbeidsforholdClient
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkSikkerhetsbegrensning
import org.slf4j.LoggerFactory
import java.time.LocalDate

class ArbeidsforholdService(private val arbeidsforholdClient: ArbeidsforholdClient) {

    companion object {
        private val log = LoggerFactory.getLogger(ArbeidsforholdService::class.java)
    }

    fun finnArbeidsforhold(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom).toEither { err ->
                log.error("Error while doing arbeidsforhold lookup", err)

                when (err) {
                    is FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    is FinnArbeidsforholdPrArbeidstakerUgyldigInput -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.UkjentFeil
                }
            }.map { liste ->
                liste.mapNotNull(ArbeidDomainMapper::toArbeidsforhold)
            }.flatMap { liste ->
                liste.map { arbeidsforhold ->
                    finnHistoriskeAvtaler(arbeidsforhold).map { avtaler ->
                        arbeidsforhold.copy(
                                arbeidsavtaler = avtaler
                        )
                    }
                }.sequenceU()
            }

    private fun finnHistoriskeAvtaler(arbeidsforhold: Arbeidsforhold) =
            arbeidsforholdClient.finnHistoriskeArbeidsavtaler(arbeidsforhold.arbeidsforholdId).toEither { err ->
                log.error("Error while doing arbeidsforhold historikk lookup", err)

                when (err) {
                    is HentArbeidsforholdHistorikkSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    is HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet -> Feilårsak.IkkeFunnet
                    else -> Feilårsak.UkjentFeil
                }
            }.map { avtaler ->
                avtaler.map(ArbeidDomainMapper::toArbeidsavtale)
            }
}
