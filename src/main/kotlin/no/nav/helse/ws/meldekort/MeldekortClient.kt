package no.nav.helse.ws.meldekort

import io.prometheus.client.*
import no.nav.helse.*
import no.nav.helse.common.*
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.*
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.*
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.meldinger.*
import org.slf4j.*
import java.time.*

class MeldekortClient(val port: MeldekortUtbetalingsgrunnlagV1) {

    private val log = LoggerFactory.getLogger("MeldekortClient")
    private val counter = Counter.build()
            .name("oppslag_meldekort_utbetalingsgrunnlag")
            .labelNames("status")
            .help("Antall registeroppslag av meldekort for en person")
            .register()

    private val timer = Histogram.build()
            .name("finn_meldekort_utbetalingsgrunnlag_seconds")
            .help("latency for MeldekortUtbetalingsgrunnlagV1.finnMeldekortUtbetalingsgrunnlagListe()").register()

    fun hentMeldekortgrunnlag(aktørId: String, fom: LocalDate, tom: LocalDate): OppslagResult {
        return timer.time<OppslagResult> {
            try {
                val response: FinnMeldekortUtbetalingsgrunnlagListeResponse = port.finnMeldekortUtbetalingsgrunnlagListe(FinnMeldekortUtbetalingsgrunnlagListeRequest()
                        .apply {
                            this.ident = AktoerId().apply {
                                this.aktoerId = aktørId
                            }
                            this.periode = Periode().apply {
                                this.fom = fom.toXmlGregorianCalendar()
                                this.tom = tom.toXmlGregorianCalendar()
                            }
                            this.temaListe.add(Tema().apply { this.kodeverksRef = "DAG" })
                            this.temaListe.add(Tema().apply { this.kodeverksRef = "AAP" })
                        })

                counter.labels("success").inc()
                Success(response.meldekortUtbetalingsgrunnlagListe.flatMap(this::toSak))
            } catch (ex: Exception) {
                log.error("Error while doing meldekort lookup", ex)
                counter.labels("failure").inc()
                Failure(listOf("${ex.javaClass.simpleName} : ${ex.message}"))
            }
        }
    }

    private fun toSak(sak: Sak): List<MeldekortUtbetalingsgrunnlagSak> {
        return when {
            sak.vedtakListe.isEmpty() -> sakUtenVedtak(sak)
            else -> sakMedVedtakListe(sak)
        }
    }

    private fun sakMedVedtakListe(sak: Sak): List<MeldekortUtbetalingsgrunnlagSak> {
        return sak.vedtakListe.map { vedtak -> sakMedVedtak(sak, vedtak) }
    }

    private fun sakMedVedtak(sak: Sak, vedtak: Vedtak): MeldekortUtbetalingsgrunnlagSak {
        return MeldekortUtbetalingsgrunnlagSak(
                type = sak.tema.kodeverksRef,
                kilde = "ARENA",
                saksnummer = sak.fagsystemSakId,
                saksstatus = sak.saksstatus.kodeverksRef,
                kravMottattDato = vedtak.datoKravMottatt.toLocalDate(),
                vedtaksstatus = vedtak.vedtaksstatus.kodeverksRef,
                vedtakFom = vedtak.vedtaksperiode.fom.toLocalDate(),
                vedtakTom = vedtak.vedtaksperiode.tom.toLocalDate(),
                meldekort = vedtak.meldekortListe.map(this::toMeldekort)
        )
    }

    private fun toMeldekort(meldekort: Meldekort): MeldekortForVedtak {
        return MeldekortForVedtak(
                fom = meldekort.meldekortperiode.fom.toLocalDate(),
                tom = meldekort.meldekortperiode.tom.toLocalDate(),
                belop = meldekort.beloep,
                dagsats = meldekort.dagsats,
                utbetalingsgrad = meldekort.utbetalingsgrad
        )
    }

    private fun sakUtenVedtak(sak: Sak): List<MeldekortUtbetalingsgrunnlagSak> {
        return listOf(MeldekortUtbetalingsgrunnlagSak(
                type = sak.tema.kodeverksRef,
                kilde = "ARENA",
                saksnummer = sak.fagsystemSakId,
                saksstatus = sak.saksstatus.kodeverksRef
        ))
    }

}

data class MeldekortUtbetalingsgrunnlagSak(
        val type: String,
        val kilde: String,
        val saksnummer: String,
        val saksstatus: String,
        val kravMottattDato: LocalDate? = null,
        val vedtaksstatus: String? = null,
        val meldekort: List<MeldekortForVedtak> = emptyList(),
        val vedtakFom: LocalDate? = null,
        val vedtakTom: LocalDate? = null
)

data class MeldekortForVedtak(
        val fom: LocalDate,
        val tom: LocalDate,
        val belop: Double,
        val dagsats: Double,
        val utbetalingsgrad: Double
)

