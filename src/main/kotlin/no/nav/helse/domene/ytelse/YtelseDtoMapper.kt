package no.nav.helse.domene.ytelse

import no.nav.helse.domene.ytelse.domain.Beregningsgrunnlag
import no.nav.helse.domene.ytelse.domain.InfotrygdSakOgGrunnlag
import no.nav.helse.domene.ytelse.domain.Utbetalingsvedtak
import no.nav.helse.domene.ytelse.domain.Ytelse
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
                    sak = InfotrygdSakDto(
                            sakId = sakOgGrunnlag.sak.sakId,
                            iverksatt = sakOgGrunnlag.sak.iverksatt,
                            tema = sakOgGrunnlag.sak.tema.name(),
                            behandlingstema = sakOgGrunnlag.sak.behandlingstema.name(),
                            opphørerFom = sakOgGrunnlag.sak.opphørerFom
                    ),
                    grunnlag = sakOgGrunnlag.grunnlag.map(::toDto)
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
