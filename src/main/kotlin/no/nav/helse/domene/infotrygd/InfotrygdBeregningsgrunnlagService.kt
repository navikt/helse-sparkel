package no.nav.helse.domene.infotrygd

import arrow.core.flatMap
import arrow.core.leftIfNull
import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagListeClient
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListePersonIkkeFunnet
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeUgyldigInput
import org.slf4j.LoggerFactory
import java.time.LocalDate

class InfotrygdBeregningsgrunnlagService(private val infotrygdClient : InfotrygdBeregningsgrunnlagListeClient, private val aktørregisterService: AktørregisterService) {

    companion object {
        private val log = LoggerFactory.getLogger(InfotrygdBeregningsgrunnlagService::class.java)
    }

    fun finnGrunnlagListe(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            aktørregisterService.fødselsnummerForAktør(aktørId).flatMap { fnr ->
                infotrygdClient.finnGrunnlagListe(Fødselsnummer(fnr), fom, tom).toEither { err ->
                    log.error("Error while doing infotrygdBeregningsgrunnlag lookup", err)

                    when (err) {
                        is FinnGrunnlagListeSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                        is FinnGrunnlagListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                        is FinnGrunnlagListePersonIkkeFunnet -> Feilårsak.IkkeFunnet
                        else -> Feilårsak.UkjentFeil
                    }
                }.leftIfNull {
                    log.info("FinnGrunnlagListeResponse er null")
                    Feilårsak.FeilFraTjeneste
                }
            }
}
