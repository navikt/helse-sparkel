package no.nav.helse.ws.sykepengegrunnlag

import no.nav.helse.ws.AktørId
import no.nav.helse.ws.inntekt.InntektService
import java.time.YearMonth

class SykepengegrunnlagService(private val inntektService: InntektService) {

    companion object {
        private const val Sammenligningsgrunnlagfilter = "8-30"
        private const val Beregningsgrunnlagfilter = "8-28"
    }

    fun hentBeregningsgrunnlag(aktørId: AktørId, fom: YearMonth, tom: YearMonth) =
            inntektService.hentInntekter(aktørId, fom, tom, Beregningsgrunnlagfilter)

    fun hentSammenligningsgrunnlag(aktørId: AktørId, fom: YearMonth, tom: YearMonth) =
            inntektService.hentInntekter(aktørId, fom, tom, Sammenligningsgrunnlagfilter)
}
