package no.nav.helse.domene.ytelse.infotrygd

import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.ytelse.domain.Behandlingstema
import no.nav.helse.domene.ytelse.domain.Beregningsgrunnlag
import no.nav.helse.domene.ytelse.domain.UgyldigBeregningsgrunnlagException
import no.nav.helse.domene.ytelse.domain.Utbetalingsvedtak
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.*
import org.slf4j.LoggerFactory

object BeregningsgrunnlagMapper {

    private val log = LoggerFactory.getLogger(BeregningsgrunnlagMapper::class.java)

    fun toBeregningsgrunnlag(grunnlag: Grunnlag) =
            try {
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
            } catch (err: UgyldigBeregningsgrunnlagException) {
                log.info("feil med beregningsgrunnlag, hopper over", err)
                null
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
            }.sortedBy {
                it.fom
            }
}
