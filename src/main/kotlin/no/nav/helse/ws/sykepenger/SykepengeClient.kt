package no.nav.helse.ws.sykepenger

import io.prometheus.client.Counter
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.tjeneste.virksomhet.sykepenger.v2.binding.SykepengerV2
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Periode
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Sykmeldingsperiode
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Vedtak
import no.nav.tjeneste.virksomhet.sykepenger.v2.meldinger.HentSykepengerListeRequest
import no.nav.tjeneste.virksomhet.sykepenger.v2.meldinger.HentSykepengerListeResponse
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
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
            Success(remoteResult?.toSykepengerVedtak(aktorId))
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

data class SykepengerVedtak(val fom: LocalDate,
                            val tom: LocalDate,
                            val grad: Float,
                            val mottaker: String,
                            val beløp: BigDecimal = BigDecimal.ZERO) // <- I can't find where to get this

fun HentSykepengerListeResponse.toSykepengerVedtak(aktorId: String): Collection<SykepengerVedtak> {
    return when {
        this.sykmeldingsperiodeListe.isEmpty() -> emptyList()
        else -> this.sykmeldingsperiodeListe.flatMap { it.toSykepengerVedtak(aktorId) }
    }
}

fun Sykmeldingsperiode.toSykepengerVedtak(aktorId: String): Collection<SykepengerVedtak> {
    return when {
        this.vedtakListe.isEmpty() -> emptyList<SykepengerVedtak>()
        else -> this.vedtakListe.map { it.toSykepengerVedtak(aktorId) }
    }
}

fun Vedtak.toSykepengerVedtak(aktorId: String): SykepengerVedtak {
    return SykepengerVedtak(fom = this.vedtak.fom.toLocalDate(),
            tom = this.vedtak.tom.toLocalDate(),
            beløp = BigDecimal.ONE,
            mottaker = aktorId,
            grad = this.utbetalingsgrad.toFloat())
}

fun XMLGregorianCalendar.toLocalDate(): LocalDate {
    return LocalDate.of(this.year, this.month, this.day)
}
