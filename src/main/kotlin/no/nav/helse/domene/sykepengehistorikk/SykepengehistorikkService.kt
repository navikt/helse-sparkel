package no.nav.helse.domene.sykepengehistorikk

import arrow.core.flatMap
import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.ytelse.BeregningsgrunnlagMapper
import no.nav.helse.domene.ytelse.domain.Utbetalingsvedtak
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagClient
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListePersonIkkeFunnet
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeUgyldigInput
import org.slf4j.LoggerFactory
import java.time.LocalDate


class SykepengehistorikkService(private val infotrygdBeregningsgrunnlagClient: InfotrygdBeregningsgrunnlagClient,
                                private val aktørregisterService: AktørregisterService) {

    companion object {
        private val log = LoggerFactory.getLogger(SykepengehistorikkService::class.java)
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
                response.sykepengerListe.mapNotNull {
                    try {
                        BeregningsgrunnlagMapper.toBeregningsgrunnlag(it)
                    } catch (err: IllegalArgumentException) {
                        log.info("feil med beregningsgrunnlag, hopper over", err)
                        null
                    }
                }.flatMap {
                    it.vedtak
                }.filter {
                    it is Utbetalingsvedtak.SkalUtbetales && it.utbetalingsgrad > 0
                }
            }
}
