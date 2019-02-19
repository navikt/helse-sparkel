package no.nav.helse.ws.organisasjon

import io.ktor.http.*
import no.nav.helse.*
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.*
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.*

private val SUPPORTERTE_ATTRIBUTTER = listOf(OrganisasjonsAttributt("navn"))

class OrganisasjonClient(private val organisasjonV5: OrganisasjonV5) {
    
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
                OppslagResult.Ok(OrganisasjonsMapper.fraNoekkelInfo(response))
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




