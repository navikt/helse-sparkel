package no.nav.helse.ws.sykepenger

import no.nav.helse.Either
import no.nav.helse.common.toLocalDate
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.sykepenger.v2.binding.SykepengerV2
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Periode
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Sykmeldingsperiode
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Vedtak
import no.nav.tjeneste.virksomhet.sykepenger.v2.meldinger.HentSykepengerListeRequest
import no.nav.tjeneste.virksomhet.sykepenger.v2.meldinger.HentSykepengerListeResponse
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate

class SykepengerClient(private val sykepenger: SykepengerV2) {

    private val log = LoggerFactory.getLogger("SykepengeClient")

    fun finnSykepengeVedtak(fnr: Fødselsnummer, fraOgMed: LocalDate, tilOgMed: LocalDate): Either<Exception, Collection<SykepengerVedtak>> {
        val request = createSykepengerListeRequest(fnr.value, fraOgMed, tilOgMed)
        return try {
            val remoteResult = sykepenger.hentSykepengerListe(request)
            Either.Right(remoteResult.toSykepengerVedtak(fnr.value))
        } catch (ex: Exception) {
            log.error("Error while doing sak og behndling lookup", ex)
            Either.Left(ex)
        }
    }

    fun createSykepengerListeRequest(fnr: String, fraOgMed: LocalDate, tilOgMed: LocalDate): HentSykepengerListeRequest {
        return HentSykepengerListeRequest()
                .apply { ident = fnr }
                .apply {
                    sykmelding = Periode()
                            .apply { fom = fraOgMed.toXmlGregorianCalendar() }
                            .apply { tom = tilOgMed.toXmlGregorianCalendar() }
                }
    }
}

data class SykepengerVedtak(val fom: LocalDate,
                            val tom: LocalDate,
                            val grad: Float,
                            val mottaker: String,
                            val beløp: BigDecimal = BigDecimal.ZERO) // <- I can't find where to get this

fun HentSykepengerListeResponse.toSykepengerVedtak(fnr: String): Collection<SykepengerVedtak> {
    return when {
        this.sykmeldingsperiodeListe.isEmpty() -> emptyList()
        else -> this.sykmeldingsperiodeListe.flatMap { it.toSykepengerVedtak(fnr) }
    }
}

fun Sykmeldingsperiode.toSykepengerVedtak(fnr: String): Collection<SykepengerVedtak> {
    return when {
        this.vedtakListe.isEmpty() -> emptyList()
        else -> this.vedtakListe.map { it.toSykepengerVedtak(fnr) }
    }
}

fun Vedtak.toSykepengerVedtak(fnr: String): SykepengerVedtak {
    return SykepengerVedtak(fom = this.vedtak.fom.toLocalDate(),
            tom = this.vedtak.tom.toLocalDate(),
            beløp = BigDecimal.ONE,
            mottaker = fnr,
            grad = this.utbetalingsgrad.toFloat())
}

