package no.nav.helse.ws.organisasjon

import io.prometheus.client.Counter
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.tjeneste.virksomhet.organisasjon.v5.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.SammensattNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonRequest
import org.slf4j.LoggerFactory

class OrganisasjonClient(private val organisasjonV5: OrganisasjonV5) {

    private val counter = Counter.build()
            .name("oppslag_organisasjon")
            .labelNames("status")
            .help("Antall registeroppslag av organisasjoner")
            .register()

    private val log = LoggerFactory.getLogger("OrganisasjonClient")

    fun orgNavn(orgnr: String): OppslagResult {
        val request = HentOrganisasjonRequest().apply { orgnummer = orgnr }
        return try {
            val response = organisasjonV5.hentOrganisasjon(request)
            counter.labels("success").inc()
            response.organisasjon?.navn?.let { Success(name(it)) } ?: Failure(listOf("org $orgnr not found"))
        } catch (ex: Exception) {
            log.error("Error while doing organisasjon lookup", ex)
            counter.labels("failure").inc()
            Failure(listOf(ex.message ?: "unknown error"))
        }
    }

    private fun name(sammensattNavn: SammensattNavn): String {
        return UstrukturertNavn::class.java.cast(sammensattNavn).navnelinje
                .filterNot { it.isNullOrBlank() }
                .joinToString(", ")

    }
}




