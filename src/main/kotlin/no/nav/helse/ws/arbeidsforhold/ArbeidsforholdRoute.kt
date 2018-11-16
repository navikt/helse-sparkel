package no.nav.helse.ws.arbeidsforhold

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold

fun Routing.arbeidsforhold(arbeidsforholdClient: ArbeidsforholdClient) {
    post("api/person/arbeidsforhold") {
        val fnr = call.receive<String>()
        val arbeidsforhold: Collection<Arbeidsforhold> = arbeidsforholdClient.finnArbeidsforholdForFnr(Fødselsnummer(fnr))
        call.respond(arbeidsforhold)
    }
}
