package no.nav.helse.domene.arbeid

import arrow.core.flatMap
import no.nav.helse.Feilårsak
import no.nav.helse.arrow.sequenceU
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.arbeid.domain.Arbeidsforhold
import no.nav.helse.oppslag.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.oppslag.inntekt.InntektClient
import no.nav.helse.probe.DatakvalitetProbe
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkUgyldigInput
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class ArbeidsforholdService(private val arbeidsforholdClient: ArbeidsforholdClient,
                            private val inntektClient: InntektClient,
                            private val datakvalitetProbe: DatakvalitetProbe) {

    companion object {
        private val log = LoggerFactory.getLogger(ArbeidsforholdService::class.java)
    }

    fun finnArbeidsforhold(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            hentFrilansarbeidsforhold(aktørId, YearMonth.from(fom), YearMonth.from(tom)).flatMap { frilansArbeidsforholdliste ->
                finnArbeidstakerarbeidsforhold(aktørId, fom, tom).map { arbeidsforholdliste ->
                    arbeidsforholdliste.plus(frilansArbeidsforholdliste)
                }
            }

    fun finnArbeidstakerarbeidsforhold(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
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
            }.map { arbeidsforholdliste ->
                arbeidsforholdliste.onEach { arbeidsforhold ->
                    datakvalitetProbe.inspiserArbeidstaker(arbeidsforhold)
                }
            }

    fun hentFrilansarbeidsforhold(aktørId: AktørId, fom: YearMonth, tom: YearMonth) =
            inntektClient.hentFrilansArbeidsforhold(aktørId, fom, tom).toEither { err ->
                log.error("Error during inntekt lookup", err)

                when (err) {
                    is HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter -> Feilårsak.FeilFraTjeneste
                    is HentInntektListeBolkUgyldigInput -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.UkjentFeil
                }
            }.map {
                it.map { arbeidsforholdFrilanser ->
                    ArbeidDomainMapper.toArbeidsforhold(arbeidsforholdFrilanser)
                }.filterNotNull().also {
                    datakvalitetProbe.frilansArbeidsforhold(it)
                }
            }.map { arbeidsforholdliste ->
                arbeidsforholdliste.onEach { arbeidsforhold ->
                    datakvalitetProbe.inspiserFrilans(arbeidsforhold)
                }
            }

    private fun finnHistoriskeAvtaler(arbeidsforhold: Arbeidsforhold.Arbeidstaker) =
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
