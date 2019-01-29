package no.nav.helse.ws.organisasjon

import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.SammensattNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonRequest
import org.slf4j.LoggerFactory

class OrganisasjonClient(private val organisasjonV5: OrganisasjonV5) {

    private val log = LoggerFactory.getLogger("OrganisasjonClient")

    fun orgNavn(orgnr: String): OppslagResult {
        val request = HentOrganisasjonRequest().apply { orgnummer = orgnr }
        return try {
            val response = organisasjonV5.hentOrganisasjon(request)

            return Success(OrganisasjonResponse(response.organisasjon?.navn?.let(this::name)))
        } catch (ex: Exception) {
            log.error("Error while doing organisasjon lookup", ex)
            Failure(listOf(ex.message ?: "unknown error"))
        }
    }

    private fun name(sammensattNavn: SammensattNavn): String {
        return (sammensattNavn as UstrukturertNavn).navnelinje
                .filterNot {  it.isNullOrBlank() }
                .joinToString(", ")

    }

    data class OrganisasjonResponse(val navn: String?)
}




