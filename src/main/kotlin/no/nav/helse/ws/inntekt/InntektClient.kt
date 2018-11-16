package no.nav.helse.ws.inntekt

import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.hentinntektliste.ArbeidsInntektMaaned
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.PersonIdent
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeRequest

class InntektClient(private val inntektV3: InntektV3) {
    fun hentInntektListe(fødelsnummer: String): HentInntektListeResponse {
        val request = HentInntektListeRequest()

        request.ident = PersonIdent().apply {
            personIdent = fødelsnummer
        }

        val response = inntektV3.hentInntektListe(request)

        return HentInntektListeResponse(
                response.arbeidsInntektIdent.arbeidsInntektMaaned
        )
    }

    data class HentInntektListeResponse(
            val inntekter: List<ArbeidsInntektMaaned>
    )
}
