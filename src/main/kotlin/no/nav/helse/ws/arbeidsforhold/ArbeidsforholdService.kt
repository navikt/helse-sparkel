package no.nav.helse.ws.arbeidsforhold

import arrow.core.flatMap
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.helse.Feilårsak
import no.nav.helse.arrow.sequenceU
import no.nav.helse.common.toLocalDate
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.arbeidsforhold.client.ArbeidsforholdClient
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold
import no.nav.helse.ws.inntekt.client.InntektClient
import no.nav.helse.ws.inntekt.domain.Virksomhet
import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkUgyldigInput
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.AktoerId
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.PersonIdent
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class ArbeidsforholdService(private val arbeidsforholdClient: ArbeidsforholdClient,
                            private val inntektClient: InntektClient) {

    companion object {
        private val log = LoggerFactory.getLogger(ArbeidsforholdService::class.java)

        private val arbeidsforholdHistogram = Histogram.build()
                .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 15.0, 20.0, 40.0, 60.0, 80.0, 100.0)
                .name("arbeidsforhold_sizes")
                .help("fordeling over hvor mange arbeidsforhold en arbeidstaker har, både frilans og vanlig arbeidstaker")
                .register()

        private val frilansCounter = Counter.build()
                .name("arbeidsforhold_frilans_totals")
                .help("antall frilans arbeidsforhold")
                .register()

        private val arbeidsforholdISammeVirksomhetCounter = Counter.build()
                .name("arbeidsforhold_i_samme_virksomhet_totals")
                .help("antall arbeidsforhold i samme virksomhet")
                .register()
    }

    fun finnArbeidsforhold(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            hentFrilansarbeidsforhold(aktørId, YearMonth.from(fom), YearMonth.from(tom)).flatMap { frilansArbeidsforholdliste ->
                finnArbeidstakerarbeidsforhold(aktørId, fom, tom).map { arbeidsforholdliste ->
                    arbeidsforholdliste.plus(frilansArbeidsforholdliste).also { kombinertListe ->
                        tellAvvikPåArbeidsforholdISammeVirksomhet(kombinertListe)
                        arbeidsforholdHistogram.observe(kombinertListe.size.toDouble())
                    }
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
                    frilansCounter.inc()

                    when (arbeidsforholdFrilanser.arbeidsgiver) {
                        is PersonIdent -> Virksomhet.Person((arbeidsforholdFrilanser.arbeidsgiver as PersonIdent).personIdent)
                        is AktoerId -> Virksomhet.NavAktør((arbeidsforholdFrilanser.arbeidsgiver as AktoerId).aktoerId)
                        is Organisasjon -> Virksomhet.Organisasjon(Organisasjonsnummer((arbeidsforholdFrilanser.arbeidsgiver as Organisasjon).orgnummer))
                        else -> null
                    }?.let { virksomhet ->
                        Arbeidsforhold.Frilans(
                                arbeidsgiver = virksomhet,
                                startdato = arbeidsforholdFrilanser.frilansPeriode.fom.toLocalDate(),
                                sluttdato = arbeidsforholdFrilanser.frilansPeriode?.tom?.toLocalDate(),
                                yrke = arbeidsforholdFrilanser.yrke?.value
                        )
                    }
                }.filterNotNull()
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

    private fun tellAvvikPåArbeidsforholdISammeVirksomhet(arbeidsforholdliste: List<Arbeidsforhold>) {
        val unikeArbeidsgivere = arbeidsforholdliste.distinctBy {
            it.arbeidsgiver
        }
        val arbeidsforholdISammeVirksomhet = arbeidsforholdliste.size - unikeArbeidsgivere.size

        if (arbeidsforholdISammeVirksomhet > 0) {
            arbeidsforholdISammeVirksomhetCounter.inc(arbeidsforholdISammeVirksomhet.toDouble())
            log.info("fant $arbeidsforholdISammeVirksomhet arbeidsforhold i samme virksomhet")
        }
    }
}
