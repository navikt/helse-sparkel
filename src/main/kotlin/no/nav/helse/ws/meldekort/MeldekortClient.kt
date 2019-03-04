package no.nav.helse.ws.meldekort

import no.nav.helse.Either
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.MeldekortUtbetalingsgrunnlagV1
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.ObjectFactory
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Periode
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.meldinger.FinnMeldekortUtbetalingsgrunnlagListeRequest
import org.slf4j.LoggerFactory
import java.time.LocalDate

class MeldekortClient(private val port: MeldekortUtbetalingsgrunnlagV1) {

    private val log = LoggerFactory.getLogger("MeldekortClient")

    fun hentMeldekortgrunnlag(aktørId: String, fom: LocalDate, tom: LocalDate) =
            try {
                val request = FinnMeldekortUtbetalingsgrunnlagListeRequest()
                        .apply {
                            this.ident = AktoerId().apply {
                                this.aktoerId = aktørId
                            }
                            this.periode = Periode().apply {
                                this.fom = fom.toXmlGregorianCalendar()
                                this.tom = tom.toXmlGregorianCalendar()
                            }
                            this.temaListe.add(ObjectFactory().createTema().apply {
                                this.kodeverksRef = "DAG"
                                this.value = "DAG"
                            })
                            this.temaListe.add(ObjectFactory().createTema().apply {
                                this.kodeverksRef = "AAP"
                                this.value = "AAP"
                            })
                        }
                Either.Right(port.finnMeldekortUtbetalingsgrunnlagListe(request).meldekortUtbetalingsgrunnlagListe.toList())
            } catch (ex: Exception) {
                log.error("Error while doing meldekort lookup", ex)
                Either.Left(ex)
            }



}

