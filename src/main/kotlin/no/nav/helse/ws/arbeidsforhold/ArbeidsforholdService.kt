package no.nav.helse.ws.arbeidsforhold

import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.common.toLocalDate
import no.nav.helse.map
import no.nav.helse.orElse
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.organisasjon.OrganisasjonService
import no.nav.helse.ws.organisasjon.OrganisasjonsAttributt
import no.nav.helse.ws.organisasjon.OrganisasjonsNummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon
import java.time.LocalDate

class ArbeidsforholdService(private val arbeidsforholdClient: ArbeidsforholdClient, private val organisasjonService: OrganisasjonService) {

    fun finnArbeidsforhold(aktørId: AktørId, fom: LocalDate, tom: LocalDate): OppslagResult<Feil, List<Arbeidsforhold>> {
        val lookupResult = arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom)

        return when (lookupResult) {
            is OppslagResult.Feil -> lookupResult
            is OppslagResult.Ok -> {
                lookupResult.map { list ->
                    list.map { arbeidsforhold ->
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
                }
            }
        }
    }

    fun finnArbeidsgivere(aktørId: AktørId, fom: LocalDate, tom: LocalDate): OppslagResult<Feil, List<Arbeidsgiver>> {
        val lookupResult = arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom)

        return when (lookupResult) {
            is OppslagResult.Feil -> lookupResult
            is OppslagResult.Ok -> {
                lookupResult.data.map {
                    it.arbeidsgiver
                }.filter {
                    it is Organisasjon
                }.map {
                    it as Organisasjon
                }.distinctBy {
                    it.orgnummer
                }.map {
                    Arbeidsgiver.Organisasjon(it.orgnummer, it.navn ?: "")
                }.map { organisasjon ->
                    if (organisasjon.navn.isBlank()) {
                        organisasjonService.hentOrganisasjon(
                                orgnr = OrganisasjonsNummer(organisasjon.organisasjonsnummer),
                                attributter = listOf(OrganisasjonsAttributt("navn"))
                        ).map { organisasjonResponse ->
                            Arbeidsgiver.Organisasjon(organisasjon.organisasjonsnummer, organisasjonResponse.navn ?: "")
                        }
                    } else {
                        OppslagResult.Ok(organisasjon)
                    }
                }.let {
                    OppslagResult.Ok(it.map { oppslagResultat ->
                        when (oppslagResultat) {
                            is OppslagResult.Ok -> oppslagResultat.data
                            is OppslagResult.Feil -> {
                                return@let oppslagResultat.copy()
                            }
                        }
                    })
                }
            }
        }
    }
}

sealed class Arbeidsgiver {
    data class Organisasjon(val organisasjonsnummer: String, val navn: String): Arbeidsgiver()
}

data class Arbeidsforhold(val arbeidsgiver: Arbeidsgiver, val startdato: LocalDate, val sluttdato: LocalDate? = null)