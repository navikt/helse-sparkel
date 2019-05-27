package no.nav.helse.domene.ytelse

import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.ytelse.domain.Behandlingstema
import no.nav.helse.domene.ytelse.domain.Beregningsgrunnlag
import no.nav.helse.domene.ytelse.domain.Utbetalingsvedtak
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.*

object BeregningsgrunnlagMapper {

    fun toBeregningsgrunnlag(grunnlag: Grunnlag) =
            when (grunnlag) {
                is Sykepenger -> Beregningsgrunnlag.Sykepenger(
                        identdato = grunnlag.identdato.toLocalDate(),
                        periodeFom = grunnlag.periode?.fom?.toLocalDate(),
                        periodeTom = grunnlag.periode?.tom?.toLocalDate(),
                        behandlingstema = Behandlingstema.fraKode(grunnlag.behandlingstema.value),
                        vedtak = toVedtak(grunnlag.vedtakListe)
                )
                is Foreldrepenger -> Beregningsgrunnlag.Foreldrepenger(
                        identdato = grunnlag.identdato.toLocalDate(),
                        periodeFom = grunnlag.periode?.fom?.toLocalDate(),
                        periodeTom = grunnlag.periode?.tom?.toLocalDate(),
                        behandlingstema = Behandlingstema.fraKode(grunnlag.behandlingstema.value),
                        vedtak = toVedtak(grunnlag.vedtakListe)
                )
                is Engangsstoenad -> Beregningsgrunnlag.Engangstønad(
                        identdato = grunnlag.identdato.toLocalDate(),
                        periodeFom = grunnlag.periode?.fom?.toLocalDate(),
                        periodeTom = grunnlag.periode?.tom?.toLocalDate(),
                        behandlingstema = Behandlingstema.fraKode(grunnlag.behandlingstema.value),
                        vedtak = toVedtak(grunnlag.vedtakListe)
                )
                is PaaroerendeSykdom -> Beregningsgrunnlag.PårørendeSykdom(
                        identdato = grunnlag.identdato.toLocalDate(),
                        periodeFom = grunnlag.periode?.fom?.toLocalDate(),
                        periodeTom = grunnlag.periode?.tom?.toLocalDate(),
                        behandlingstema = Behandlingstema.fraKode(grunnlag.behandlingstema.value),
                        vedtak = toVedtak(grunnlag.vedtakListe)
                )
                else -> throw IllegalArgumentException("grunnlag er av ukjent type: ${grunnlag.javaClass.name}")
            }

    fun toVedtak(vedtakliste: List<Vedtak>) =
            vedtakliste.map { vedtak ->
                vedtak.utbetalingsgrad?.let {
                    Utbetalingsvedtak.SkalUtbetales(
                            fom = vedtak.anvistPeriode.fom.toLocalDate(),
                            tom = vedtak.anvistPeriode.tom.toLocalDate(),
                            utbetalingsgrad = vedtak.utbetalingsgrad
                    )
                } ?: Utbetalingsvedtak.SkalIkkeUtbetales(
                        fom = vedtak.anvistPeriode.fom.toLocalDate(),
                        tom = vedtak.anvistPeriode.tom.toLocalDate()
                )
            }
}
