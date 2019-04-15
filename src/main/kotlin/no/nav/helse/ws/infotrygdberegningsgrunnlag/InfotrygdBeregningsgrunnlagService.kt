package no.nav.helse.ws.infotrygdberegningsgrunnlag

import arrow.core.flatMap
import no.nav.helse.Feilårsak
import no.nav.helse.http.aktør.AktørregisterService
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.Fødselsnummer
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
                }
            }
}
