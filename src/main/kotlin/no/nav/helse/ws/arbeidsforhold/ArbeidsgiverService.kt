package no.nav.helse.ws.arbeidsforhold

import arrow.core.orNull
import no.nav.helse.Feilårsak
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.arbeidsforhold.client.ArbeidsforholdClient
import no.nav.helse.ws.organisasjon.OrganisasjonService
import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon
import org.slf4j.LoggerFactory
import java.time.LocalDate

class ArbeidsgiverService(private val arbeidsforholdClient: ArbeidsforholdClient, private val organisasjonService: OrganisasjonService) {

    companion object {
        private val log = LoggerFactory.getLogger(ArbeidsgiverService::class.java)
    }

    fun finnArbeidsgivere(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom).toEither{ err ->
                log.error("Error while doing arbeidsgivere lookup", err)

                when (err) {
                    is FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    is FinnArbeidsforholdPrArbeidstakerUgyldigInput -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.UkjentFeil
                }
            }.map { liste ->
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
            }

    private fun hentOrganisasjonsnavn(organisasjon: Organisasjon) =
            organisasjon.navn ?: organisasjonService.hentOrganisasjon(Organisasjonsnummer(organisasjon.orgnummer)).map {
                it.navn
            }.orNull()
}
