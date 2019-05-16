package no.nav.helse.oppslag.arena

import arrow.core.Try
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.AktørId
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.MeldekortUtbetalingsgrunnlagV1
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Periode
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Tema
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.meldinger.FinnMeldekortUtbetalingsgrunnlagListeRequest
import java.time.LocalDate

class MeldekortUtbetalingsgrunnlagClient(private val port: MeldekortUtbetalingsgrunnlagV1) {

    fun finnMeldekortUtbetalingsgrunnlag(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            Try {
                port.finnMeldekortUtbetalingsgrunnlagListe(finnMeldekortUtbetalingsgrunnlagListeRequest(aktørId, fom, tom))
            }

    private fun finnMeldekortUtbetalingsgrunnlagListeRequest(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            FinnMeldekortUtbetalingsgrunnlagListeRequest().apply {
                ident = AktoerId().apply {
                    aktoerId = aktørId.aktor
                }
                periode = Periode().apply {
                    this.fom = fom.toXmlGregorianCalendar()
                    this.tom = tom.toXmlGregorianCalendar()
                }
                with (temaListe) {
                    add(Tema().apply {
                        value = "DAG"
                    })
                    add(Tema().apply {
                        value = "AAP"
                    })
                }
            }
}
