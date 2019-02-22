package no.nav.helse.ws.arbeidsforhold

import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.map
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.organisasjon.OrganisasjonService
import no.nav.helse.ws.organisasjon.OrganisasjonsAttributt
import no.nav.helse.ws.organisasjon.OrganisasjonsNummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon
import java.time.LocalDate

class ArbeidsforholdService(private val arbeidsforholdClient: ArbeidsforholdClient, private val organisasjonService: OrganisasjonService) {

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