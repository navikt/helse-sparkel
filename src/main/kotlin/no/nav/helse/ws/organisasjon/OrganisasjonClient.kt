package no.nav.helse.ws.organisasjon

import io.ktor.http.HttpStatusCode
import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentNoekkelinfoOrganisasjonRequest
import org.slf4j.LoggerFactory

private val SUPPORTERTE_ATTRIBUTTER = listOf(OrganisasjonsAttributt("navn"))

class OrganisasjonClient(private val organisasjonV5: OrganisasjonV5) {

    private val log = LoggerFactory.getLogger("OrganisasjonClient")

    fun hentOrganisasjon(
            orgnr: OrganisasjonsNummer,
            attributter : List<OrganisasjonsAttributt> = listOf()
    ) : OppslagResult<Feil, OrganisasjonResponse> {
        return if (SUPPORTERTE_ATTRIBUTTER.containsAll(attributter)){
            // Bruk HentNoekkelinfoOrganisasjonRequest så fremt attriutter som krever HentOrganisasjonRequest ikke er etterspurt
            // Nå er kun navn implementert, så bruker bare HentNoekkelinfoOrganisasjonRequest
            val request = HentNoekkelinfoOrganisasjonRequest().apply { orgnummer = orgnr.value }
            try {
                val response = organisasjonV5.hentNoekkelinfoOrganisasjon(request)
                OppslagResult.Ok(OrganisasjonsMapper.fraNoekkelInfo(response, attributter))
            } catch (cause : Throwable) {
                OppslagResult.Feil(HttpStatusCode.InternalServerError, Feil.Exception(cause.message ?: "Ingen detaljer", cause))
            }
        } else {
            OppslagResult.Feil(HttpStatusCode.NotImplemented, Feil.Feilmelding("Støtter ikke alle etterspurte attributter."))
        }
    }
}

data class OrganisasjonResponse(val navn: String?)
data class OrganisasjonsNummer(val value : String)
data class OrganisasjonsAttributt(val value : String)




