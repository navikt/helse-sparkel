package no.nav.helse.ws.organisasjon

import no.nav.helse.Either
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentNoekkelinfoOrganisasjonRequest
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentNoekkelinfoOrganisasjonResponse
import org.slf4j.LoggerFactory

private val SUPPORTERTE_ATTRIBUTTER = listOf(OrganisasjonsAttributt("navn"))

class OrganisasjonClient(private val organisasjonV5: OrganisasjonV5) {

    private val log = LoggerFactory.getLogger("OrganisasjonClient")

    fun hentOrganisasjon(
            orgnr: OrganisasjonsNummer,
            attributter : List<OrganisasjonsAttributt> = listOf()
    ) : Either<Exception, HentNoekkelinfoOrganisasjonResponse> {
        return if (SUPPORTERTE_ATTRIBUTTER.containsAll(attributter)){
            // Bruk HentNoekkelinfoOrganisasjonRequest så fremt attriutter som krever HentOrganisasjonRequest ikke er etterspurt
            // Nå er kun navn implementert, så bruker bare HentNoekkelinfoOrganisasjonRequest
            val request = HentNoekkelinfoOrganisasjonRequest().apply { orgnummer = orgnr.value }
            try {
                Either.Right(organisasjonV5.hentNoekkelinfoOrganisasjon(request))
            } catch (err : Exception) {
                log.error("Error during organisasjon lookup", err)
                Either.Left(err)
            }
        } else {
            Either.Left(UkjentAttributtException())
        }
    }
}

class UkjentAttributtException: Exception()

data class OrganisasjonsNummer(val value : String)
data class OrganisasjonsAttributt(val value : String)




