package no.nav.helse.domene.aiy.aareg

import arrow.core.orNull
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.aiy.organisasjon.OrganisasjonService
import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjonsnummer
import no.nav.helse.oppslag.arbeidsforhold.ArbeidsforholdClient
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon
import java.time.LocalDate

class ArbeidsgiverService(private val arbeidsforholdClient: ArbeidsforholdClient,
                          private val organisasjonService: OrganisasjonService) {

    fun finnArbeidsgivere(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom)
                    .toEither(AaregErrorMapper::mapToError)
                    .map { liste ->
                        liste.map { arbeidsforhold ->
                            arbeidsforhold.arbeidsgiver
                        }.filter { aktør ->
                            aktør is Organisasjon
                        }.map { aktør ->
                            aktør as Organisasjon
                        }.distinctBy { organisasjon ->
                            organisasjon.orgnummer
                        }.map { organisasjon ->
                            no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Virksomhet(Organisasjonsnummer(organisasjon.orgnummer), hentOrganisasjonsnavn(organisasjon))
                        }
                    }

    private fun hentOrganisasjonsnavn(organisasjon: Organisasjon) =
            organisasjon.navn ?: organisasjonService.hentOrganisasjon(Organisasjonsnummer(organisasjon.orgnummer)).map {
                it.navn
            }.orNull()
}
