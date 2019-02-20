package no.nav.helse.ws.inntekt

import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.http.aktør.AktørregisterService
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektIdent
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse
import java.time.YearMonth

class InntektService(private val inntektClient: InntektClient, private val aktørregisterService: AktørregisterService) {

    fun hentInntekter(aktørId: AktørId, fom: YearMonth, tom: YearMonth): OppslagResult<Feil, HentInntektListeBolkResponse> {
        val fnr = Fødselsnummer(aktørregisterService.fødselsnummerForAktør(aktørId))

        return inntektClient.hentInntektListe(fnr, fom, tom)
    }
}
