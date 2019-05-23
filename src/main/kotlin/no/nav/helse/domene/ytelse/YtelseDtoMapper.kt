package no.nav.helse.domene.ytelse

import no.nav.helse.domene.ytelse.domain.InfotrygdSakOgGrunnlag
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
                    grunnlag = sakOgGrunnlag.grunnlag.map { beregningsgrunnlag ->
                        BeregningsgrunnlagDto(
                                type = beregningsgrunnlag.type(),
                                identdato = beregningsgrunnlag.identdato,
                                periodeFom = beregningsgrunnlag.periodeFom,
                                periodeTom = beregningsgrunnlag.periodeTom,
                                behandlingstema = beregningsgrunnlag.behandlingstema.name(),
                                vedtak = beregningsgrunnlag.vedtak.map { beregningsgrunnlagVedtak ->
                                    BeregningsgrunnlagVedtakDto(
                                            fom = beregningsgrunnlagVedtak.fom,
                                            tom = beregningsgrunnlagVedtak.tom,
                                            utbetalingsgrad = beregningsgrunnlagVedtak.utbetalingsgrad
                                    )
                                }
                        )
                    }
            )
}
