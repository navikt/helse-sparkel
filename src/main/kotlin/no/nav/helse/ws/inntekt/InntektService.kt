package no.nav.helse.ws.inntekt

import no.nav.helse.Either
import no.nav.helse.Feilårsak
import no.nav.helse.bimap
import no.nav.helse.common.toLocalDate
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkUgyldigInput
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.AktoerId
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektIdent
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

    private fun hentInntekt(aktørId: AktørId, fom: YearMonth, tom: YearMonth, f: InntektService.() -> Either<Exception, HentInntektListeBolkResponse>) =
            f().bimap({
                when (it) {
                    is HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter -> Feilårsak.FeilFraTjeneste
                    is HentInntektListeBolkUgyldigInput -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.UkjentFeil
                }
            }, {
                    InntektMapper.mapToInntekt(aktørId, fom, tom, it.arbeidsInntektIdentListe)
            })
}

class UgyldigOpptjeningsperiodeException(message: String) : Exception(message)

data class Opptjeningsperiode(val fom: LocalDate, val tom: LocalDate, val antattPeriode: Boolean = false) {
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
    fun mapToInntekt(aktørId: AktørId, fom: YearMonth, tom: YearMonth, arbeidsInntektIdentListe: List<ArbeidsInntektIdent>) =
            arbeidsInntektIdentListe.flatMap {
                it.arbeidsInntektMaaned
            }.flatMap {
                it.arbeidsInntektInformasjon.inntektListe
            }.filter(fjernAndreAktører(aktørId))
            .filter(fjernInntektUtenforPeriode(fom, tom))
            .filter(fjernAndreArbeidsgivereEnnVirksomheter())
            .map { inntekt ->
                val arbeidsgiver = Arbeidsgiver.Organisasjon((inntekt.opplysningspliktig as Organisasjon).orgnummer)
                try {
                    Inntekt(
                            arbeidsgiver = arbeidsgiver,
                            opptjeningsperiode = opptjeningsperiode(inntekt),
                            beløp = inntekt.beloep)
                } catch (err: UgyldigOpptjeningsperiodeException) {
                    null
                }
            }.filterNotNull()

    private fun fjernAndreAktører(aktørId: AktørId) = { inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt ->
        inntekt.inntektsmottaker is AktoerId && (inntekt.inntektsmottaker as AktoerId).aktoerId == aktørId.aktor
    }

    private fun fjernInntektUtenforPeriode(fom: YearMonth, tom: YearMonth) = { inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt ->
        erOpptjeningsperioderInnenforPeriode(fom, tom, inntekt) || erUtbetalingsperiodeInnenforPeriode(fom, tom, inntekt)
    }

    private fun harOpptjeningsperiode(inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt) = inntekt.opptjeningsperiode != null

    private fun opptjeningsperiode(inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt) =
            if (harOpptjeningsperiode(inntekt)) {
                Opptjeningsperiode(
                        fom = inntekt.opptjeningsperiode.startDato.toLocalDate(),
                        tom = inntekt.opptjeningsperiode.sluttDato.toLocalDate())
            } else {
                antaAtInntektGjelderDenMånedenDetRapporteresForNårOpptjeningsperiodenErTom(inntekt)
            }

    private fun antaAtInntektGjelderDenMånedenDetRapporteresForNårOpptjeningsperiodenErTom(inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt) =
            Opptjeningsperiode(
                    fom = inntekt.utbetaltIPeriode.toLocalDate().withDayOfMonth(1),
                    tom = YearMonth.from(inntekt.utbetaltIPeriode.toLocalDate()).atEndOfMonth(),
                    antattPeriode = true
            )

    private fun erOpptjeningsperioderInnenforPeriode(fom: YearMonth, tom: YearMonth, inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt) =
        harOpptjeningsperiode(inntekt)
                && fom.atDay(1) <= inntekt.opptjeningsperiode.startDato.toLocalDate()
                && tom.atEndOfMonth() >= inntekt.opptjeningsperiode.sluttDato.toLocalDate()

    private fun erUtbetalingsperiodeInnenforPeriode(fom: YearMonth, tom: YearMonth, inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt) =
        !harOpptjeningsperiode(inntekt)
                && fom.atDay(1) <= inntekt.utbetaltIPeriode.toLocalDate()
                && tom.atEndOfMonth() >= inntekt.utbetaltIPeriode.toLocalDate()

    private fun fjernAndreArbeidsgivereEnnVirksomheter() = { inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt ->
        inntekt.opplysningspliktig is Organisasjon
    }
}
