package no.nav.helse.domene.sykepengehistorikk

import no.nav.helse.Feilårsak
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListePersonIkkeFunnet
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.FinnGrunnlagListeUgyldigInput
import org.slf4j.LoggerFactory
import javax.xml.ws.soap.SOAPFaultException

object DomainErrorMapper {

    private val log = LoggerFactory.getLogger(DomainErrorMapper::class.java)

    fun mapToError(err: Throwable) =
            with (err) {
                log.error("received error during lookup", this)

                when (this) {
                    is FinnGrunnlagListeSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    is FinnGrunnlagListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                    is FinnGrunnlagListePersonIkkeFunnet -> Feilårsak.IkkeFunnet
                    is SOAPFaultException -> when (message) {
                        "Basene i Infotrygd er ikke tilgjengelige" -> Feilårsak.TjenesteErUtilgjengelig
                        else -> Feilårsak.UkjentFeil
                    }
                    else -> Feilårsak.UkjentFeil
                }
            }


}
