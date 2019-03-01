package no.nav.helse.ws.arbeidsforhold

import no.nav.helse.Feilårsak
import no.nav.helse.bimap
import no.nav.helse.common.toLocalDate
import no.nav.helse.fold
import no.nav.helse.map
import no.nav.helse.mapLeft
import no.nav.helse.orElse
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.organisasjon.OrganisasjonService
import no.nav.helse.ws.organisasjon.OrganisasjonsAttributt
import no.nav.helse.ws.organisasjon.OrganisasjonsNummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput
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
            }, {
                it.map { arbeidsforhold ->
                    Arbeidsforhold(arbeidsforhold.arbeidsgiver.let { aktør ->
                        when (aktør) {
                            is Organisasjon -> {
                                val navn = aktør.navn ?: organisasjonService.hentOrganisasjonnavn(OrganisasjonsNummer(aktør.orgnummer)).orElse { "FEIL VED HENTING AV NAVN" }
                                Arbeidsgiver.Organisasjon(aktør.orgnummer, navn)
                            }

                            else -> Arbeidsgiver.Organisasjon("0000000000", "UKJENT ARBEIDSGIVERTYPE")
                        }
                    }, arbeidsforhold.ansettelsesPeriode.periode.fom.toLocalDate(), arbeidsforhold.ansettelsesPeriode.periode.tom?.toLocalDate())
                }
            })

    fun finnArbeidsgivere(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom).mapLeft{
                when (it) {
                    is FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    is FinnArbeidsforholdPrArbeidstakerUgyldigInput -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.UkjentFeil
                }
            }.map {
                it.map {
                    it.arbeidsgiver
                }.filter {
                    it is Organisasjon
                }.map {
                    it as Organisasjon
                }.distinctBy {
                    it.orgnummer
                }.map { organisasjon ->
                    Arbeidsgiver.Organisasjon(organisasjon.orgnummer, organisasjon.navn ?: organisasjonService.hentOrganisasjon(
                            orgnr = OrganisasjonsNummer(organisasjon.orgnummer),
                            attributter = listOf(OrganisasjonsAttributt("navn"))
                    ).fold({ null }, { it.navn }))
                }
            }
}

sealed class Arbeidsgiver {
    data class Organisasjon(val organisasjonsnummer: String, val navn: String?): Arbeidsgiver()
}

data class Arbeidsforhold(val arbeidsgiver: Arbeidsgiver, val startdato: LocalDate, val sluttdato: LocalDate? = null)
