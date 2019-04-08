package no.nav.helse.ws.infotrygdberegningsgrunnlag

import no.nav.helse.Either
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeRequest
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.meldinger.FinnGrunnlagListeResponse
import org.slf4j.LoggerFactory
import java.time.LocalDate

class InfotrygdBeregningsgrunnlagListeClient(private val infotrygdBeregningsgrunnlag : InfotrygdBeregningsgrunnlagV1) {

    private val log = LoggerFactory.getLogger("InfotrygdBeregningsgrunnlagListeClient")

    fun finnGrunnlagListe(fnr: Fødselsnummer, fraOgMed: LocalDate, tilOgMed: LocalDate): Either<Exception, FinnGrunnlagListeResponse> {
        val request = createFinnGrunnlagListeRequest(fnr.value, fraOgMed, tilOgMed)
        return try {
            Either.Right(infotrygdBeregningsgrunnlag.finnGrunnlagListe(request))
        } catch (ex: Exception) {
            log.error("Error while doing sak og behndling lookup", ex)
            Either.Left(ex)
        }
    }

    fun createFinnGrunnlagListeRequest(fnr: String, fraOgMed: LocalDate, tilOgMed: LocalDate): FinnGrunnlagListeRequest {
        return FinnGrunnlagListeRequest()
                .apply {
                    personident = fnr
                    fom = fraOgMed.toXmlGregorianCalendar()
                    tom = tilOgMed.toXmlGregorianCalendar()
                }
    }

}