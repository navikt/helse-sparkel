package no.nav.helse.domene.aiy.aareg

import no.nav.helse.Feilårsak
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkSikkerhetsbegrensning
import org.slf4j.LoggerFactory

object AaregErrorMapper {

    private val log = LoggerFactory.getLogger(AaregErrorMapper::class.java)

    fun mapToError(err: Throwable) =
            when (err) {
                is FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                is FinnArbeidsforholdPrArbeidstakerUgyldigInput -> Feilårsak.FeilFraTjeneste
                is HentArbeidsforholdHistorikkSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                is HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet -> Feilårsak.IkkeFunnet
                else -> Feilårsak.UkjentFeil
            }.also {
                log.error("received error during lookup, mapping to $it", err)
            }
}
