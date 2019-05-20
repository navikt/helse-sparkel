package no.nav.helse.domene.ytelse

import arrow.core.flatMap
import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.infotrygd.InfotrygdBeregningsgrunnlagService
import no.nav.helse.domene.ytelse.domain.Beregningsgrunnlag
import no.nav.helse.domene.ytelse.domain.InfotrygdSak
import no.nav.helse.domene.ytelse.domain.InfotrygdSakOgGrunnlag
import no.nav.helse.domene.ytelse.domain.Ytelser
import no.nav.helse.oppslag.arena.MeldekortUtbetalingsgrunnlagClient
import no.nav.helse.oppslag.infotrygd.InfotrygdSakClient
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagListeClient
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListePersonIkkeFunnet
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeUgyldigInput
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.FinnSakListePersonIkkeFunnet
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.FinnSakListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.FinnSakListeUgyldigInput
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeAktoerIkkeFunnet
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeUgyldigInput
import org.slf4j.LoggerFactory
import java.time.LocalDate

class YtelseService(private val aktørregisterService: AktørregisterService,
                    private val infotrygdBeregningsgrunnlagListeClient: InfotrygdBeregningsgrunnlagListeClient,
                    private val infotrygdSakClient: InfotrygdSakClient,
                    private val meldekortUtbetalingsgrunnlagClient: MeldekortUtbetalingsgrunnlagClient) {

    companion object {
        private val log = LoggerFactory.getLogger(InfotrygdBeregningsgrunnlagService::class.java)
    }

    fun finnYtelser(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            finnYtelserFraArena(aktørId, fom, tom).flatMap { ytelserFraArena ->
                finnYtelserFraInfotrygd(aktørId, fom, tom).map { ytelserFraInfotrygd ->
                    Ytelser(arena = ytelserFraArena, infotrygd = ytelserFraInfotrygd)
                }
            }

    private fun finnYtelserFraInfotrygd(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            aktørregisterService.fødselsnummerForAktør(aktørId).flatMap { fnr ->
                infotrygdSakClient.finnSakListe(fnr, fom, tom).toEither { err ->
                    log.error("Error while doing infotrygdSak lookup", err)

                    when (err) {
                        is FinnSakListeSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                        is FinnSakListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                        is FinnSakListePersonIkkeFunnet -> Feilårsak.IkkeFunnet
                        else -> Feilårsak.UkjentFeil
                    }
                }.map { finnSakListeResponse ->
                    finnSakListeResponse.sakListe.map(InfotrygdSakMapper::toSak)
                            .plus(finnSakListeResponse.vedtakListe.map(InfotrygdSakMapper::toSak))
                }.flatMap { saker ->
                    infotrygdBeregningsgrunnlagListeClient.finnGrunnlagListe(Fødselsnummer(fnr), fom, tom).toEither { err ->
                        log.error("Error while doing infotrygdBeregningsgrunnlag lookup", err)

                        when (err) {
                            is FinnGrunnlagListeSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                            is FinnGrunnlagListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                            is FinnGrunnlagListePersonIkkeFunnet -> Feilårsak.IkkeFunnet
                            else -> Feilårsak.UkjentFeil
                        }
                    }.map { finnGrunnlagListeResponse ->
                        with (finnGrunnlagListeResponse) {
                            sykepengerListe.map(BeregningsgrunnlagMapper::toBeregningsgrunnlag)
                                    .plus(foreldrepengerListe.map(BeregningsgrunnlagMapper::toBeregningsgrunnlag))
                                    .plus(engangstoenadListe.map(BeregningsgrunnlagMapper::toBeregningsgrunnlag))
                                    .plus(paaroerendeSykdomListe.map(BeregningsgrunnlagMapper::toBeregningsgrunnlag))
                        }
                    }.map { grunnlagliste ->
                        val (sakerMedGrunnlag, grunnlagUtenSak) = sammenstillSakerOgGrunnlag(saker, grunnlagliste)

                        if (grunnlagUtenSak.isNotEmpty()) {
                            grunnlagUtenSak.forEach { grunnlag ->
                                log.info("finner ikke sak for grunnlag $grunnlag")
                            }
                        }

                        sakerMedGrunnlag
                    }
                }
            }

    private fun sammenstillSakerOgGrunnlag(saker: List<InfotrygdSak>, grunnlagliste: List<Beregningsgrunnlag>): Pair<List<InfotrygdSakOgGrunnlag>, List<Beregningsgrunnlag>> {
        val grunnlagUtenSak = grunnlagliste.toMutableList()

        return saker.map { sak ->
            val grunnlag = grunnlagUtenSak.filter { grunnlag ->
                grunnlag.hørerSammenMed(sak)
            }

            grunnlagUtenSak.removeAll(grunnlag)

            InfotrygdSakOgGrunnlag(sak, grunnlag)
        }.let { sakerMedGrunnlag ->
            sakerMedGrunnlag to grunnlagUtenSak
        }
    }

    private fun finnYtelserFraArena(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
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
                        YtelseMapper.fraArena(sak, vedtak)
                    }
                }
            }
}
