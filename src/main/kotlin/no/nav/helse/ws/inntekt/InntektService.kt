package no.nav.helse.ws.inntekt

import no.nav.helse.Feilårsak
import no.nav.helse.OppslagResult
import no.nav.helse.common.toLocalDate
import no.nav.helse.map
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkUgyldigInput
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.AktoerId
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class InntektService(private val inntektClient: InntektClient) {

    fun hentBeregningsgrunnlag(aktørId: AktørId, fom: YearMonth, tom: YearMonth) = hentInntekt(aktørId, fom, tom) {
        inntektClient.hentBeregningsgrunnlag(aktørId, fom, tom)
    }

    fun hentSammenligningsgrunnlag(aktørId: AktørId, fom: YearMonth, tom: YearMonth) = hentInntekt(aktørId, fom, tom) {
        inntektClient.hentSammenligningsgrunnlag(aktørId, fom, tom)
    }

    private fun hentInntekt(aktørId: AktørId, fom: YearMonth, tom: YearMonth, f: InntektService.() -> OppslagResult<Exception, HentInntektListeBolkResponse>): OppslagResult<Feilårsak, List<Inntekt>> {
        val lookupResult = f()
        return when (lookupResult) {
            is OppslagResult.Ok -> lookupResult.map(InntektMapper.mapToInntekt(aktørId, fom, tom))
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

class UgyldigOpptjeningsperiodeException(message: String) : Exception(message)

data class Opptjeningsperiode(val fom: LocalDate, val tom: LocalDate) {
    init {
        if (fom.withDayOfMonth(1) != tom.withDayOfMonth(1)) {
            throw UgyldigOpptjeningsperiodeException("Opptjeningsperiode kan ikke strekke seg over flere måneder")
        }
        if (tom < fom) {
            throw UgyldigOpptjeningsperiodeException("tom < fom")
        }
    }
}
sealed class Arbeidsgiver {
    data class Organisasjon(val orgnr: String): Arbeidsgiver()
}
data class Inntekt(val arbeidsgiver: Arbeidsgiver, val opptjeningsperiode: Opptjeningsperiode, val beløp: BigDecimal)

object InntektMapper {
    fun mapToInntekt(aktørId: AktørId, fom: YearMonth, tom: YearMonth): (HentInntektListeBolkResponse) -> List<Inntekt> {
        return { hentInntektListeBolkResponse ->
            hentInntektListeBolkResponse.arbeidsInntektIdentListe.flatMap {
                it.arbeidsInntektMaaned
            }.flatMap {
                it.arbeidsInntektInformasjon.inntektListe
            }.filter(fjernAndreAktører(aktørId))
            .filter(fjernOpptjeningsperioderUtenforPeriode(fom, tom))
            .filter(fjernAndreArbeidsgivereEnnVirksomheter())
            .map {
                val arbeidsgiver = Arbeidsgiver.Organisasjon((it.opplysningspliktig as Organisasjon).orgnummer)
                try {
                    Inntekt(arbeidsgiver, Opptjeningsperiode(it.opptjeningsperiode.startDato.toLocalDate(),
                            it.opptjeningsperiode.sluttDato.toLocalDate()), it.beloep)
                } catch (err: UgyldigOpptjeningsperiodeException) {
                    null
                }
            }.filterNotNull()
        }
    }

    private fun fjernAndreAktører(aktørId: AktørId) = { inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt ->
        inntekt.inntektsmottaker is AktoerId && (inntekt.inntektsmottaker as AktoerId).aktoerId == aktørId.aktor
    }

    private fun fjernOpptjeningsperioderUtenforPeriode(fom: YearMonth, tom: YearMonth) = { inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt ->
        inntekt.opptjeningsperiode.startDato.toLocalDate() >= fom.atDay(1)
                && inntekt.opptjeningsperiode.sluttDato.toLocalDate() <= tom.atEndOfMonth()
    }

    private fun fjernAndreArbeidsgivereEnnVirksomheter() = { inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt ->
        inntekt.opplysningspliktig is Organisasjon
    }
}
