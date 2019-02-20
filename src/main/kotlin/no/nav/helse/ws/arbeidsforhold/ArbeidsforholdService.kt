package no.nav.helse.ws.arbeidsforhold

import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.http.aktør.AktørregisterService
import no.nav.helse.map
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.Fødselsnummer
import no.nav.helse.ws.organisasjon.OrganisasjonService
import no.nav.helse.ws.organisasjon.OrganisasjonsAttributt
import no.nav.helse.ws.organisasjon.OrganisasjonsNummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon
import java.time.LocalDate

class ArbeidsforholdService(private val arbeidsforholdClient: ArbeidsforholdClient, private val aktørregisterService: AktørregisterService, private val organisasjonService: OrganisasjonService) {

    fun finnArbeidsgivere(aktørId: AktørId, fom: LocalDate, tom: LocalDate): OppslagResult<Feil, List<Organisasjon>> {
        val fnr = Fødselsnummer(aktørregisterService.fødselsnummerForAktør(aktørId))

        val lookupResult = arbeidsforholdClient.finnArbeidsforhold(fnr, fom, tom)

        return when (lookupResult) {
            is OppslagResult.Feil -> lookupResult
            is OppslagResult.Ok -> {
                lookupResult.data.map {
                    it.arbeidsgiver
                }.filter {
                    it is Organisasjon
                }.map {
                    it as Organisasjon
                }.map { organisasjon ->
                    if (organisasjon.navn.isNullOrBlank()) {
                        organisasjonService.hentOrganisasjon(
                                orgnr = OrganisasjonsNummer(organisasjon.orgnummer),
                                attributter = listOf(OrganisasjonsAttributt("navn"))
                        ).map { organisasjonResponse ->
                            Organisasjon().apply {
                                navn = organisasjonResponse.navn
                                orgnummer = organisasjon.orgnummer
                            }
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
