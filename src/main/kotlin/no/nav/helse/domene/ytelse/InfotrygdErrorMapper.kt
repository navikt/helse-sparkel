package no.nav.helse.domene.ytelse

import no.nav.helse.Feilårsak
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.BaseneErUtilgjengeligeException
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.FinnSakListePersonIkkeFunnet
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.FinnSakListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.FinnSakListeUgyldigInput
import org.slf4j.LoggerFactory

object InfotrygdErrorMapper {

    private val log = LoggerFactory.getLogger(InfotrygdErrorMapper::class.java)

    fun mapToError(err: Throwable) =
            when (err) {
                is FinnSakListeSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                is FinnSakListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                is FinnSakListePersonIkkeFunnet -> Feilårsak.IkkeFunnet
                is BaseneErUtilgjengeligeException -> Feilårsak.TjenesteErUtilgjengelig
                else -> Feilårsak.UkjentFeil
            }.also {
                log.info("received error during lookup, mapping to $it", err)
            }
}
