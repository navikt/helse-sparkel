package no.nav.helse.ws.sykepengehistorikk

import io.prometheus.client.Histogram
import no.nav.helse.common.toLocalDate
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit


class SykepengehistorikkService(val infotrygdService : InfotrygdBeregningsgrunnlagService) {

    companion object {
        private val log = LoggerFactory.getLogger(SykepengehistorikkService::class.java)

        private val tidligereSykepengedagerHistogram = Histogram.build()
                .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 15.0, 20.0, 30.0, 40.0, 60.0, 80.0, 100.0, 200.0, 248.0, 250.0, 260.0, 300.0, 365.0)
                .name("sykepengehistorikk_sizes")
                .help("fordeling over hvor mange tidligere anviste sykepengedager en arbeidstaker har")
                .register()
    }

    fun hentSykepengeHistorikk(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            infotrygdService.finnGrunnlagListe(aktørId, fom, tom).bimap({
                it
            }, {
                val result = it.sykepengerListe.flatMap {
                    it.vedtakListe.filter { it.utbetalingsgrad != null && it.utbetalingsgrad > 0 }.map {
                        Tidsperiode(it.anvistPeriode.fom.toLocalDate(), it.anvistPeriode.tom.toLocalDate())
                    }
                }
                tidligereSykepengedagerHistogram.observe(result.map { it.nrOfDays() }.reduce(operation = Long::plus).toDouble())
                result
            })
}

data class Tidsperiode(val fom: LocalDate, val tom: LocalDate) {

    init {
        if (tom.isBefore(fom)) throw IllegalArgumentException("tom cannot be before fom, $tom is before $fom")
    }

    fun contains(day: LocalDate): Boolean = !(day.isBefore(fom) || day.isAfter(tom))


    fun days(): List<LocalDate> = (0 until nrOfDays()).map(fom::plusDays)

    internal fun nrOfDays() = if (fom == tom ) 1 else ChronoUnit.DAYS.between(fom, tom) + 1
}