package no.nav.helse.ws.sykepenger

import io.prometheus.client.Counter
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.tjeneste.virksomhet.sykepenger.v2.binding.SykepengerV2
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Periode
import no.nav.tjeneste.virksomhet.sykepenger.v2.meldinger.HentSykepengerListeRequest
import no.nav.tjeneste.virksomhet.sykepenger.v2.meldinger.HentSykepengerListeResponse
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

class SykepengerClient(private val sykepenger: SykepengerV2) {

    private val log = LoggerFactory.getLogger("SykepengeClient")

    private val counter = Counter.build()
            .name("oppslag_sykepenger")
            .labelNames("status")
            .help("Antall registeroppslag for hent sykepengeliste")
            .register()

    fun finnSykepengeVedtak(aktorId: String, fraOgMed: DateTime, tilOgMed: DateTime): OppslagResult {
        val request = createSykepengerListeRequest(aktorId, fraOgMed, tilOgMed)

        return try {
            val remoteResult: HentSykepengerListeResponse? = sykepenger.hentSykepengerListe(request)
            counter.labels("success").inc()
            Success(remoteResult)
        } catch (ex: Exception) {
            log.error("Error while doing sak og behndling lookup", ex)
            counter.labels("failure").inc()
            Failure(listOf(ex.message ?: "unknown error"))
        }
    }
    
    fun createSykepengerListeRequest(aktorId: String, fraOgMed: DateTime, tilOgMed: DateTime): HentSykepengerListeRequest {
        val request = HentSykepengerListeRequest()
                .apply { this.ident = aktorId }
                .apply {
                    this.sykmelding = Periode()
                            .apply { this.fom = toCal(fraOgMed) }
                            .apply { this.tom = toCal(tilOgMed) }
                }
        return request
    }

    fun toCal(dt: DateTime): XMLGregorianCalendar {
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(dt.toGregorianCalendar())
    }
}
