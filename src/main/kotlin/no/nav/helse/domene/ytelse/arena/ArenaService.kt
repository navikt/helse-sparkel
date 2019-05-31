package no.nav.helse.domene.ytelse.arena

import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.oppslag.arena.MeldekortUtbetalingsgrunnlagClient
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeAktoerIkkeFunnet
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeUgyldigInput
import org.slf4j.LoggerFactory
import java.time.LocalDate

class ArenaService(private val meldekortUtbetalingsgrunnlagClient: MeldekortUtbetalingsgrunnlagClient) {

    companion object {
        private val log = LoggerFactory.getLogger(ArenaService::class.java)
    }

    fun finnSaker(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            meldekortUtbetalingsgrunnlagClient.finnMeldekortUtbetalingsgrunnlag(aktørId, fom, tom).toEither { err ->
                log.error("Error while doing meldekortUtbetalingsgrunnlag lookup", err)

                when (err) {
                    is FinnMeldekortUtbetalingsgrunnlagListeSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    is FinnMeldekortUtbetalingsgrunnlagListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                    is FinnMeldekortUtbetalingsgrunnlagListeAktoerIkkeFunnet -> Feilårsak.IkkeFunnet
                    else -> Feilårsak.UkjentFeil
                }
            }.map {
                it.meldekortUtbetalingsgrunnlagListe
            }.map { saker ->
                saker.flatMap { sak ->
                    sak.vedtakListe.map { vedtak ->
                        ArenaSakMapper.fraArena(sak, vedtak)
                    }
                }
            }
}
