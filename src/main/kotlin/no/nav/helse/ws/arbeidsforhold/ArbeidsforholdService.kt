package no.nav.helse.ws.arbeidsforhold

import no.nav.helse.*
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.arbeidsforhold.client.ArbeidsforholdClient
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold
import no.nav.helse.ws.organisasjon.OrganisasjonService
import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon
import java.time.LocalDate

class ArbeidsforholdService(private val arbeidsforholdClient: ArbeidsforholdClient, private val organisasjonService: OrganisasjonService) {

    fun finnArbeidsforhold(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom).bimap({
                when (it) {
                    is FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    is FinnArbeidsforholdPrArbeidstakerUgyldigInput -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.UkjentFeil
                }
            }, { liste ->
                liste.mapNotNull(ArbeidDomainMapper::toArbeidsforhold)
            }).flatMap { liste ->
                liste.map { arbeidsforhold ->
                    finnHistoriskeAvtaler(arbeidsforhold).map { avtaler ->
                        arbeidsforhold.copy(
                                arbeidsavtaler = avtaler
                        )
                    }
                }.sequenceU()
            }

    private fun finnHistoriskeAvtaler(arbeidsforhold: Arbeidsforhold) =
            arbeidsforholdClient.finnHistoriskeArbeidsavtaler(arbeidsforhold.arbeidsforholdId).bimap({
                when (it) {
                    is HentArbeidsforholdHistorikkSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    is HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet -> Feilårsak.IkkeFunnet
                    else -> Feilårsak.UkjentFeil
                }
            }, { avtaler ->
                avtaler.map(ArbeidDomainMapper::toArbeidsavtale)
            })

    fun finnArbeidsgivere(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom).bimap({ exception ->
                when (exception) {
                    is FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    is FinnArbeidsforholdPrArbeidstakerUgyldigInput -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.UkjentFeil
                }
            }, { liste ->
                liste.map { arbeidsforhold ->
                    arbeidsforhold.arbeidsgiver
                }.filter { aktør ->
                    aktør is Organisasjon
                }.map { aktør ->
                    aktør as Organisasjon
                }.distinctBy { organisasjon ->
                    organisasjon.orgnummer
                }.map { organisasjon ->
                    no.nav.helse.ws.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer(organisasjon.orgnummer), hentOrganisasjonsnavn(organisasjon))
                }
            })

    private fun hentOrganisasjonsnavn(organisasjon: Organisasjon) =
            organisasjon.navn ?: organisasjonService.hentOrganisasjon(Organisasjonsnummer(organisasjon.orgnummer)).map {
                it.navn
            }.orElse { null }
}
