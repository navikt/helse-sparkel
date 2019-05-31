package no.nav.helse.domene.ytelse.infotrygd

import no.nav.helse.Feilårsak
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.BaseneErUtilgjengeligeException
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListePersonIkkeFunnet
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeUgyldigInput
import org.slf4j.LoggerFactory

object InfotrygdBeregningsgrunnlagErrorMapper {

    private val log = LoggerFactory.getLogger(InfotrygdBeregningsgrunnlagErrorMapper::class.java)

    fun mapToError(err: Throwable) =
            when (err) {
                is FinnGrunnlagListeSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                is FinnGrunnlagListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                is FinnGrunnlagListePersonIkkeFunnet -> Feilårsak.IkkeFunnet
                is BaseneErUtilgjengeligeException -> Feilårsak.TjenesteErUtilgjengelig
                else -> Feilårsak.UkjentFeil
            }.also {
                log.info("received error during lookup, mapping to $it", err)
            }
}
