package no.nav.helse.domene.organisasjon

import arrow.core.Either
import arrow.core.flatMap
import no.nav.helse.Feilårsak
import no.nav.helse.oppslag.organisasjon.OrganisasjonClient
import no.nav.helse.domene.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentOrganisasjonOrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentOrganisasjonUgyldigInput
import org.slf4j.LoggerFactory
import java.time.LocalDate

class OrganisasjonService(private val organisasjonsClient: OrganisasjonClient) {

    companion object {
        private val log = LoggerFactory.getLogger(OrganisasjonService::class.java)
    }

    fun hentOrganisasjon(orgnr: Organisasjonsnummer) =
            organisasjonsClient.hentOrganisasjon(orgnr).toEither { err ->
                log.error("Error during organisasjon lookup", err)

                when (err) {
                    is HentOrganisasjonOrganisasjonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentOrganisasjonUgyldigInput -> Feilårsak.FeilFraBruker
                    else -> Feilårsak.UkjentFeil
                }
            }.flatMap {
                OrganisasjonsMapper.fraOrganisasjon(it)?.let {
                    Either.Right(it)
                } ?: Either.Left(Feilårsak.UkjentFeil)
            }
}
