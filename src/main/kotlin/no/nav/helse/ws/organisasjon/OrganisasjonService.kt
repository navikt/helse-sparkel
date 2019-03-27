package no.nav.helse.ws.organisasjon

import no.nav.helse.Either
import no.nav.helse.Feilårsak
import no.nav.helse.bimap
import no.nav.helse.flatMap
import no.nav.helse.map
import no.nav.helse.mapLeft
import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentOrganisasjonOrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentOrganisasjonUgyldigInput
import org.slf4j.LoggerFactory
import java.time.LocalDate

class OrganisasjonService(private val organisasjonsClient: OrganisasjonClient) {

    companion object {
        private val log = LoggerFactory.getLogger("OrganisasjonService")
    }

    fun hentOrganisasjon(orgnr: Organisasjonsnummer) =
            organisasjonsClient.hentOrganisasjon(orgnr).mapLeft {
                when (it) {
                    is HentOrganisasjonOrganisasjonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is HentOrganisasjonUgyldigInput -> Feilårsak.FeilFraBruker
                    else -> Feilårsak.UkjentFeil
                }
            }.flatMap {
                OrganisasjonsMapper.fraOrganisasjon(it)?.let {
                    Either.Right(it)
                } ?: Either.Left(Feilårsak.UkjentFeil)
            }

    fun hentVirksomhetForJuridiskOrganisasjonsnummer(orgnr: Organisasjonsnummer, dato: LocalDate = LocalDate.now()) =
            organisasjonsClient.hentVirksomhetForJuridiskOrganisasjonsnummer(orgnr, dato).mapLeft {
                Feilårsak.UkjentFeil
            }.flatMap {
                it.unntakForOrgnrListe.firstOrNull()?.let {
                    // example unntaksmeldinger:
                    // - <orgnr> er opphørt eller eksisterer ikke på dato <dato>
                    // - <orgnr> er et ugyldig organisasjonsnummer
                    // - <orgnr> har flere enn en aktiv virksomhet på dato <dato>
                    log.warn("Unntaksmelding for organisasjonsnummer ${orgnr.value}: ${it.unntaksmelding}")
                    Either.Left(Feilårsak.IkkeFunnet)
                } ?: it.orgnrForOrganisasjonListe.firstOrNull {
                    it.juridiskOrganisasjonsnummer == orgnr.value
                }?.let { organisasjon ->
                    Either.Right(Organisasjonsnummer(organisasjon.organisasjonsnummer))
                } ?: Feilårsak.IkkeFunnet.let {
                    log.error("did not find virksomhet for juridisk orgnr ${orgnr.value}")
                    Either.Left(it)
                }
            }
}
