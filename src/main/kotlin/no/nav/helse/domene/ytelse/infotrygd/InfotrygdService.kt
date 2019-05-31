package no.nav.helse.domene.ytelse.infotrygd

import arrow.core.Either
import arrow.core.flatMap
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.ytelse.domain.Beregningsgrunnlag
import no.nav.helse.domene.ytelse.domain.InfotrygdSak
import no.nav.helse.domene.ytelse.domain.InfotrygdSakOgGrunnlag
import no.nav.helse.oppslag.infotrygd.InfotrygdSakClient
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagClient
import no.nav.helse.probe.DatakvalitetProbe
import org.slf4j.LoggerFactory
import java.time.LocalDate

class InfotrygdService(private val infotrygdBeregningsgrunnlagClient: InfotrygdBeregningsgrunnlagClient,
                       private val infotrygdSakClient: InfotrygdSakClient,
                       private val probe: DatakvalitetProbe) {

    companion object {
        private val log = LoggerFactory.getLogger(InfotrygdService::class.java)
    }

    fun finnGrunnlag(fødselsnummer: Fødselsnummer, fom: LocalDate, tom: LocalDate) =
            infotrygdBeregningsgrunnlagClient.finnGrunnlagListe(fødselsnummer.value, fom, tom)
                    .toEither(InfotrygdBeregningsgrunnlagErrorMapper::mapToError)
                    .map { finnGrunnlagListeResponse ->
                        with (finnGrunnlagListeResponse) {
                            sykepengerListe.map(BeregningsgrunnlagMapper::toBeregningsgrunnlag)
                                    .plus(foreldrepengerListe.map(BeregningsgrunnlagMapper::toBeregningsgrunnlag))
                                    .plus(engangstoenadListe.map(BeregningsgrunnlagMapper::toBeregningsgrunnlag))
                                    .plus(paaroerendeSykdomListe.map(BeregningsgrunnlagMapper::toBeregningsgrunnlag))
                                    .filterNotNull()
                        }
                    }

    private fun finnSaker(fødselsnummer: Fødselsnummer, fom: LocalDate, tom: LocalDate) =
            infotrygdSakClient.finnSakListe(fødselsnummer.value, fom, tom)
                    .toEither(InfotrygdErrorMapper::mapToError)
                    .map { finnSakListeResponse ->
                        finnSakListeResponse.sakListe
                                .onEach(probe::inspiserInfotrygdSak)
                                .map(InfotrygdSakMapper::toSak)
                                .plus(finnSakListeResponse.vedtakListe
                                        .onEach(probe::inspiserInfotrygdSak)
                                        .map(InfotrygdSakMapper::toSak))
                    }

    fun finnSakerOgGrunnlag(fødselsnummer: Fødselsnummer, fom: LocalDate, tom: LocalDate) =
            finnSaker(fødselsnummer, fom, tom)
                    .flatMap { saker ->
                        finnGrunnlag(fødselsnummer, fom, tom).map {
                            saker to it
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
                            log.info("finner ikke grunnlag for sak ${sakUtenGrunnlag.sak}")
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
}
