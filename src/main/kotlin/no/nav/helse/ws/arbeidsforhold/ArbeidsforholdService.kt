package no.nav.helse.ws.arbeidsforhold

import no.nav.helse.Feilårsak
import no.nav.helse.bimap
import no.nav.helse.common.toLocalDate
import no.nav.helse.map
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
            }, { liste ->
                liste.map { arbeidsforhold ->
                    arbeidsforhold.arbeidsgiver.let { aktør ->
                        when (aktør) {
                            is Organisasjon -> Arbeidsgiver.Organisasjon(aktør.orgnummer, hentOrganisasjonsnavn(aktør) ?: "FEIL VED HENTING AV NAVN")
                            else -> Arbeidsgiver.Organisasjon("0000000000", "UKJENT ARBEIDSGIVERTYPE")
                        }
                    }.let { arbeidsgiver ->
                        Arbeidsforhold(arbeidsgiver,
                                arbeidsforhold.ansettelsesPeriode.periode.fom.toLocalDate(),
                                arbeidsforhold.ansettelsesPeriode.periode.tom?.toLocalDate()
                        )
                    }
                }
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
                    Arbeidsgiver.Organisasjon(organisasjon.orgnummer, hentOrganisasjonsnavn(organisasjon))
                }
            })

    private fun hentOrganisasjonsnavn(organisasjon: Organisasjon) =
            organisasjon.navn ?: organisasjonService.hentOrganisasjon(
                    orgnr = OrganisasjonsNummer(organisasjon.orgnummer),
                    attributter = listOf(OrganisasjonsAttributt("navn"))).map {
                it.navn
            }.orElse { null }
}

sealed class Arbeidsgiver {
    data class Organisasjon(val orgnummer: String, val navn: String?): Arbeidsgiver()
}

data class Arbeidsforhold(val arbeidsgiver: Arbeidsgiver, val startdato: LocalDate, val sluttdato: LocalDate? = null)
