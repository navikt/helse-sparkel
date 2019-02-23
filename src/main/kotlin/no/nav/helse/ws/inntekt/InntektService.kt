package no.nav.helse.ws.inntekt

import no.nav.helse.ws.AktørId
import java.time.YearMonth

class InntektService(private val inntektClient: InntektClient) {

    fun hentInntekter(aktørId: AktørId, fom: YearMonth, tom: YearMonth) = inntektClient.hentInntektListe(aktørId, fom, tom)
}
