package no.nav.helse.domene.sykepengehistorikk

import arrow.core.flatMap
import io.prometheus.client.Histogram
import no.nav.helse.Feilårsak
import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagClient
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListePersonIkkeFunnet
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeUgyldigInput
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit


class SykepengehistorikkService(private val infotrygdBeregningsgrunnlagClient: InfotrygdBeregningsgrunnlagClient,
                                private val aktørregisterService: AktørregisterService) {

    companion object {
        private val log = LoggerFactory.getLogger(SykepengehistorikkService::class.java)

        private val tidligereSykepengedagerHistogram = Histogram.build()
                .buckets(0.0, 7.0, 14.0, 31.0, 100.0, 248.0, 300.0, 365.0)
                .name("sykepengehistorikk_sizes")
                .help("fordeling over hvor mange tidligere anviste sykepengedager en arbeidstaker har")
                .register()
    }

    fun hentSykepengeHistorikk(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            aktørregisterService.fødselsnummerForAktør(aktørId).flatMap { fnr ->
                infotrygdBeregningsgrunnlagClient.finnGrunnlagListe(Fødselsnummer(fnr), fom, tom).toEither { err ->
                    log.error("Error while doing infotrygdBeregningsgrunnlag lookup", err)

                    when (err) {
                        is FinnGrunnlagListeSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                        is FinnGrunnlagListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                        is FinnGrunnlagListePersonIkkeFunnet -> Feilårsak.IkkeFunnet
                        else -> Feilårsak.UkjentFeil
                    }
                }
            }.map { response ->
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
