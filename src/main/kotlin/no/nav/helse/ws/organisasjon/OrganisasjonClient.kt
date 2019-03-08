package no.nav.helse.ws.organisasjon

import no.nav.helse.Either
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonRequest
import org.slf4j.LoggerFactory

class OrganisasjonClient(private val organisasjonV5: OrganisasjonV5) {

    private val log = LoggerFactory.getLogger("OrganisasjonClient")

    fun hentOrganisasjon(orgnr: OrganisasjonsNummer) : Either<Exception, Organisasjon> {
        val request = HentOrganisasjonRequest().apply { orgnummer = orgnr.value }
        return try {
            Either.Right(organisasjonV5.hentOrganisasjon(request).organisasjon)
        } catch (err : Exception) {
            log.error("Error during organisasjon lookup", err)
            Either.Left(err)
        }
    }
}

data class OrganisasjonsNummer(val value : String)




