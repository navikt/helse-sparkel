package no.nav.helse.domene.ytelse

import arrow.core.flatMap
import no.nav.helse.Feilårsak
import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.infotrygd.InfotrygdBeregningsgrunnlagService
import no.nav.helse.domene.ytelse.domain.Ytelse
import no.nav.helse.oppslag.arena.MeldekortUtbetalingsgrunnlagClient
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeAktoerIkkeFunnet
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeUgyldigInput
import org.slf4j.LoggerFactory
import java.time.LocalDate

class YtelseService(private val infotrygdBeregningsgrunnlagService: InfotrygdBeregningsgrunnlagService,
                    private val meldekortUtbetalingsgrunnlagClient: MeldekortUtbetalingsgrunnlagClient) {

    companion object {
        private val log = LoggerFactory.getLogger(InfotrygdBeregningsgrunnlagService::class.java)
    }

    fun finnYtelser(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            finnYtelserFraArena(aktørId, fom, tom).flatMap { ytelserFraArena ->
                finnYtelserFraInfotrygd(aktørId, fom, tom).map { ytelserFraInfotrygd ->
                    ytelserFraArena + ytelserFraInfotrygd
                }
            }

    private fun finnYtelserFraInfotrygd(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            infotrygdBeregningsgrunnlagService.finnGrunnlagListe(aktørId, fom, tom)
                    .map { finnGrunnlagListeResponse ->
                        val sykepenger = finnGrunnlagListeResponse.sykepengerListe.flatMap { sykepenger ->
                            sykepenger.vedtakListe.map { vedtak ->
                                Ytelse(
                                        tema = "SYKEPENGER",
                                        fom = vedtak.anvistPeriode.fom.toLocalDate(),
                                        tom = vedtak.anvistPeriode.tom.toLocalDate()
                                )
                            }
                        }

                        val foreldrepenger = finnGrunnlagListeResponse.foreldrepengerListe.flatMap { foreldrepenger ->
                            foreldrepenger.vedtakListe.map { vedtak ->
                                Ytelse(
                                        tema = "FORELDREPENGER",
                                        fom = vedtak.anvistPeriode.fom.toLocalDate(),
                                        tom = vedtak.anvistPeriode.tom.toLocalDate()
                                )
                            }
                        }

                        val engangstønader = finnGrunnlagListeResponse.engangstoenadListe.flatMap { engangstønad ->
                            engangstønad.vedtakListe.map { vedtak ->
                                Ytelse(
                                        tema = "ENGANGSTØNAD",
                                        fom = vedtak.anvistPeriode.fom.toLocalDate(),
                                        tom = vedtak.anvistPeriode.tom.toLocalDate()
                                )
                            }
                        }

                        val pårørendeSykdom = finnGrunnlagListeResponse.paaroerendeSykdomListe.flatMap { paaroerendeSykdom ->
                            paaroerendeSykdom.vedtakListe.map {vedtak ->
                                Ytelse(
                                        tema = "PÅRØRENDESYKDOM",
                                        fom = vedtak.anvistPeriode.fom.toLocalDate(),
                                        tom = vedtak.anvistPeriode.tom.toLocalDate()
                                )
                            }
                        }

                        sykepenger + foreldrepenger + engangstønader + pårørendeSykdom
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
                        Ytelse(
                                tema = sak.tema.value,
                                fom = vedtak.vedtaksperiode.fom.toLocalDate(),
                                tom = vedtak.vedtaksperiode.tom.toLocalDate()
                        )
                    }
                }
            }
}
