package no.nav.helse.ws.meldekort

import no.nav.helse.ws.AktørId
import java.time.LocalDate

class MeldekortService(private val meldekortClient: MeldekortClient) {

    fun hentMeldekortgrunnlag(aktørId: AktørId, fom: LocalDate, tom: LocalDate) = meldekortClient.hentMeldekortgrunnlag(aktørId.aktor, fom, tom)
}
