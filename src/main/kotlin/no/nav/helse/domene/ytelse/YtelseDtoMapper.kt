package no.nav.helse.domene.ytelse

import no.nav.helse.domene.ytelse.domain.*
import no.nav.helse.domene.ytelse.dto.*

object YtelseDtoMapper {

    fun toDto(ytelse: Ytelse) =
            YtelseDto(
                    kilde = ytelse.kilde.type(),
                    tema = ytelse.tema,
                    fom = ytelse.fom,
                    tom = ytelse.tom
            )

    fun toDto(sakOgGrunnlag: InfotrygdSakOgGrunnlag) =
            InfotrygdSakOgGrunnlagDto(
                    sak = toDto(sakOgGrunnlag.sak),
                    grunnlag = sakOgGrunnlag.grunnlag?.let(::toDto)
            )

    fun toDto(sak: InfotrygdSak) =
            InfotrygdSakDto(
                    type = when (sak) {
                        is InfotrygdSak.Vedtak -> "Vedtak"
                        is InfotrygdSak.Åpen -> "Sak"
                    },
                    iverksatt = if (sak is InfotrygdSak.Vedtak) sak.iverksatt else null,
                    tema = sak.tema.name(),
                    behandlingstema = sak.behandlingstema.name(),
                    opphørerFom = if (sak is InfotrygdSak.Vedtak) sak.opphørerFom else null
            )

    fun toDto(beregningsgrunnlag: Beregningsgrunnlag) =
            BeregningsgrunnlagDto(
                    type = beregningsgrunnlag.type(),
                    identdato = beregningsgrunnlag.identdato,
                    periodeFom = beregningsgrunnlag.utbetalingFom,
                    periodeTom = beregningsgrunnlag.utbetalingTom,
                    behandlingstema = beregningsgrunnlag.behandlingstema.name(),
                    vedtak = beregningsgrunnlag.vedtak.map(::toDto)
            )

    fun toDto(utbetalingsvedtak: Utbetalingsvedtak) =
            BeregningsgrunnlagVedtakDto(
                    fom = utbetalingsvedtak.fom,
                    tom = utbetalingsvedtak.tom,
                    utbetalingsgrad = if (utbetalingsvedtak is Utbetalingsvedtak.SkalUtbetales) utbetalingsvedtak.utbetalingsgrad else null
            )

}
