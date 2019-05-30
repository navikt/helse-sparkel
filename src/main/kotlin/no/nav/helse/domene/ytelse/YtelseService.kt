package no.nav.helse.domene.ytelse

import arrow.core.Either
import arrow.core.Try
import arrow.core.flatMap
import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.sykepengehistorikk.DomainErrorMapper
import no.nav.helse.domene.ytelse.domain.Beregningsgrunnlag
import no.nav.helse.domene.ytelse.domain.InfotrygdSak
import no.nav.helse.domene.ytelse.domain.InfotrygdSakOgGrunnlag
import no.nav.helse.domene.ytelse.domain.Ytelser
import no.nav.helse.oppslag.arena.MeldekortUtbetalingsgrunnlagClient
import no.nav.helse.oppslag.infotrygd.InfotrygdSakClient
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagClient
import no.nav.helse.probe.DatakvalitetProbe
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeAktoerIkkeFunnet
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeUgyldigInput
import org.slf4j.LoggerFactory
import java.time.LocalDate

class YtelseService(private val aktørregisterService: AktørregisterService,
                    private val infotrygdBeregningsgrunnlagClient: InfotrygdBeregningsgrunnlagClient,
                    private val infotrygdSakClient: InfotrygdSakClient,
                    private val meldekortUtbetalingsgrunnlagClient: MeldekortUtbetalingsgrunnlagClient,
                    private val probe: DatakvalitetProbe) {

    companion object {
        private val log = LoggerFactory.getLogger(YtelseService::class.java)
    }

    fun finnYtelser(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            finnYtelserFraArena(aktørId, fom, tom).flatMap { ytelserFraArena ->
                finnYtelserFraInfotrygd(aktørId, fom, tom).map { ytelserFraInfotrygd ->
                    Ytelser(arena = ytelserFraArena, infotrygd = ytelserFraInfotrygd)
                }
            }

    private fun finnYtelserFraInfotrygd(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            aktørregisterService.fødselsnummerForAktør(aktørId).flatMap { fnr ->
                infotrygdSakClient.finnSakListe(fnr, fom, tom)
                        .toEither(InfotrygdErrorMapper::mapToError)
                        .map { finnSakListeResponse ->
                    finnSakListeResponse.sakListe
                            .onEach(probe::inspiserInfotrygdSak)
                            .map(InfotrygdSakMapper::toSak)
                            .plus(finnSakListeResponse.vedtakListe
                                    .onEach(probe::inspiserInfotrygdSak)
                                    .map(InfotrygdSakMapper::toSak))
                }.flatMap { saker ->
                    infotrygdBeregningsgrunnlagClient.finnGrunnlagListe(Fødselsnummer(fnr), fom, tom)
                            .toEither(DomainErrorMapper::mapToError)
                            .map {
                                saker to it
                            }
                }.map { (saker, finnGrunnlagListeResponse) ->
                    with (finnGrunnlagListeResponse) {
                        sykepengerListe.map {
                            Try {
                                BeregningsgrunnlagMapper.toBeregningsgrunnlag(it)
                            }
                        }.plus(foreldrepengerListe.map {
                            Try {
                                BeregningsgrunnlagMapper.toBeregningsgrunnlag(it)
                            }
                        }).plus(engangstoenadListe.map {
                            Try {
                                BeregningsgrunnlagMapper.toBeregningsgrunnlag(it)
                            }
                        }).plus(paaroerendeSykdomListe.map {
                            Try {
                                BeregningsgrunnlagMapper.toBeregningsgrunnlag(it)
                            }
                        }).mapNotNull {
                            it.fold({ err ->
                                log.info("feil med beregningsgrunnlag, hopper over", err)
                                null
                            }) {
                                it
                            }
                        }.let {
                            saker to it
                        }
                    }
                }.map { (saker, grunnlagliste) ->
                    val (sakerMedGrunnlag, grunnlagUtenSak) = sammenstillSakerOgGrunnlag(saker, grunnlagliste)

                    if (grunnlagUtenSak.isNotEmpty()) {
                        grunnlagUtenSak.forEach { grunnlag ->
                            log.info("finner ikke sak for grunnlag $grunnlag")
                        }
                    }

                    sakerMedGrunnlag.filter { infotrygdSakOgGrunnlag ->
                        infotrygdSakOgGrunnlag.grunnlag == null
                    }.forEach { sakUtenGrunnlag ->
                        log.info("finner ikke grunnlag for sak med sakId=${sakUtenGrunnlag.sak.sakId}")
                    }

                    sakerMedGrunnlag
                }.also { either ->
                    if (either is Either.Right) {
                        probe.inspiserInfotrygdSakerOgGrunnlag(either.b)
                    }
                }.map { sakerMedGrunnlag ->
                    sakerMedGrunnlag.filter {
                        it.grunnlag != null
                    }
                }
            }

    private fun sammenstillSakerOgGrunnlag(saker: List<InfotrygdSak>, grunnlagliste: List<Beregningsgrunnlag>): Pair<List<InfotrygdSakOgGrunnlag>, List<Beregningsgrunnlag>> {
        val grunnlagUtenSak = grunnlagliste.toMutableList()

        return saker.map { sak ->
            val grunnlag = grunnlagUtenSak.firstOrNull { grunnlag ->
                grunnlag.hørerSammenMed(sak)
            }?.also {
                grunnlagUtenSak.remove(it)
            }

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
