package no.nav.helse.domene.sykepengehistorikk

import io.prometheus.client.Histogram
import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.infotrygd.InfotrygdBeregningsgrunnlagService
import java.time.LocalDate
import java.time.temporal.ChronoUnit


class SykepengehistorikkService(val infotrygdService : InfotrygdBeregningsgrunnlagService) {

    companion object {
        private val tidligereSykepengedagerHistogram = Histogram.build()
                .buckets(0.0, 7.0, 14.0, 31.0, 100.0, 248.0, 300.0, 365.0)
                .name("sykepengehistorikk_sizes")
                .help("fordeling over hvor mange tidligere anviste sykepengedager en arbeidstaker har")
                .register()
    }

    fun hentSykepengeHistorikk(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            infotrygdService.finnGrunnlagListe(aktørId, fom, tom).map { response ->
                response.sykepengerListe.flatMap { sykepenger ->
                    sykepenger.vedtakListe.filter { vedtak ->
                        vedtak.utbetalingsgrad != null && vedtak.utbetalingsgrad > 0
                    }.map { vedtak ->
                        Tidsperiode(vedtak.anvistPeriode.fom.toLocalDate(), vedtak.anvistPeriode.tom.toLocalDate())
                    }
                }.also {
                    it.map { tidsperiode ->
                        tidsperiode.nrOfDays()
                    }.fold(0L, Long::plus).also { antallSykepengedager ->
                        tidligereSykepengedagerHistogram.observe(antallSykepengedager.toDouble())
                    }
                }
            }
}

data class Tidsperiode(val fom: LocalDate, val tom: LocalDate) {

    init {
        if (tom.isBefore(fom)) {
            throw IllegalArgumentException("tom cannot be before fom: $tom is before $fom")
        }
    }

    fun contains(day: LocalDate): Boolean = !(day.isBefore(fom) || day.isAfter(tom))

    internal fun nrOfDays() = if (fom == tom ) 1 else ChronoUnit.DAYS.between(fom, tom) + 1
}
