package no.nav.helse.ws.meldekort

import no.nav.helse.Feilårsak
import no.nav.helse.OppslagResult
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeAktoerIkkeFunnet
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeUgyldigInput
import java.time.LocalDate

class MeldekortService(private val meldekortClient: MeldekortClient) {

    fun hentMeldekortgrunnlag(aktørId: AktørId, fom: LocalDate, tom: LocalDate): OppslagResult<Feilårsak, List<MeldekortUtbetalingsgrunnlagSak>> {
        val lookupResult = meldekortClient.hentMeldekortgrunnlag(aktørId.aktor, fom, tom)
        return when (lookupResult) {
            is OppslagResult.Ok -> lookupResult
            is OppslagResult.Feil -> {
                OppslagResult.Feil(when (lookupResult.feil) {
                    is FinnMeldekortUtbetalingsgrunnlagListeAktoerIkkeFunnet -> Feilårsak.IkkeFunnet
                    is FinnMeldekortUtbetalingsgrunnlagListeSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    is FinnMeldekortUtbetalingsgrunnlagListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.FeilFraTjeneste
                })
            }
        }
    }
}
