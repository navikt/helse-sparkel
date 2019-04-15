package no.nav.helse.ws.inntekt.client

import arrow.core.Try
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Ainntektsfilter
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.AktoerId
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Formaal
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Uttrekksperiode
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest
import java.time.YearMonth


class InntektClient(private val inntektV3: InntektV3) {

    fun hentBeregningsgrunnlag(aktørId: AktørId, fom: YearMonth, tom: YearMonth) =
            Try {
                inntektV3.hentInntektListeBolk(hentInntektListeRequest(aktørId, fom, tom, Beregningsgrunnlagfilter))
            }

    fun hentSammenligningsgrunnlag(aktørId: AktørId, fom: YearMonth, tom: YearMonth) =
            Try {
                inntektV3.hentInntektListeBolk(hentInntektListeRequest(aktørId, fom, tom, Sammenligningsgrunnlagfilter))
            }

    fun hentInntekter(aktørId: AktørId, fom: YearMonth, tom: YearMonth) =
            Try {
                inntektV3.hentInntektListeBolk(hentInntektListeRequest(aktørId, fom, tom, Foreldrepengerfilter))
            }

    private fun hentInntektListeRequest(aktørId: AktørId, fom: YearMonth, tom: YearMonth, filter: String) =
            HentInntektListeBolkRequest().apply {
                identListe.add(AktoerId().apply {
                    aktoerId = aktørId.aktor
                })
                formaal = Formaal().apply {
                    value = "Foreldrepenger"
                }
                ainntektsfilter = Ainntektsfilter().apply {
                    value = filter
                }
                uttrekksperiode = Uttrekksperiode().apply {
                    maanedFom = fom.toXmlGregorianCalendar()
                    maanedTom = tom.toXmlGregorianCalendar()
                }
            }

    companion object {
        private const val Sammenligningsgrunnlagfilter = "8-30"
        private const val Beregningsgrunnlagfilter = "8-28"
        private const val Foreldrepengerfilter = "ForeldrepengerA-Inntekt"
    }
}

