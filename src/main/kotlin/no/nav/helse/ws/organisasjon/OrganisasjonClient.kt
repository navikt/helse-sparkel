package no.nav.helse.ws.organisasjon

import io.prometheus.client.*
import no.nav.helse.*
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.*
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.*
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.*
import org.slf4j.*

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




