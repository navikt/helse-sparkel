package no.nav.helse.ws.infotrygdberegningsgrunnlag

import no.nav.helse.Either
import no.nav.helse.Feilårsak
import no.nav.helse.bimap
import no.nav.helse.flatMap
import no.nav.helse.http.aktør.AktørregisterService
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListePersonIkkeFunnet
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeUgyldigInput
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeResponse
import java.time.LocalDate

class InfotrygdBeregningsgrunnlagService(private val infotrygdClient : InfotrygdBeregningsgrunnlagListeClient, private val aktørregisterService: AktørregisterService) {

    fun finnGrunnlagListe(aktørId: AktørId, fom: LocalDate, tom: LocalDate): Either<Feilårsak, FinnGrunnlagListeResponse> =
            aktørregisterService.fødselsnummerForAktør(aktørId).flatMap { fnr ->
                infotrygdClient.finnGrunnlagListe(Fødselsnummer(fnr), fom, tom).bimap({
                    when (it) {
                        is FinnGrunnlagListeSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                        is FinnGrunnlagListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                        is FinnGrunnlagListePersonIkkeFunnet -> Feilårsak.IkkeFunnet
                        else -> Feilårsak.UkjentFeil
                    }
                }, {
                    it
                })
            }
}