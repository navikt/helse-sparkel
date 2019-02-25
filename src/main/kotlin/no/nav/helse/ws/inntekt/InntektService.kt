package no.nav.helse.ws.inntekt

import no.nav.helse.Feilårsak
import no.nav.helse.OppslagResult
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkUgyldigInput
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse
import java.time.YearMonth

class InntektService(private val inntektClient: InntektClient) {

    fun hentInntekter(aktørId: AktørId, fom: YearMonth, tom: YearMonth): OppslagResult<Feilårsak, HentInntektListeBolkResponse> {
        val lookupResult = inntektClient.hentInntektListe(aktørId, fom, tom)

        return when (lookupResult) {
            is OppslagResult.Ok -> lookupResult
            is OppslagResult.Feil -> {
                OppslagResult.Feil(when (lookupResult.feil) {
                    is HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter -> Feilårsak.FeilFraTjeneste
                    is HentInntektListeBolkUgyldigInput -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.UkjentFeil
                })
            }
        }
    }
}
