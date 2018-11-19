package no.nav.helse.ws.arbeidsforhold

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.ws.Fødselsnummer

fun Routing.arbeidsforhold(arbeidsforholdClient: ArbeidsforholdClient) {
    post("api/arbeidsforhold") {
        val fnr = call.receive<String>()
        val arbeidsforhold: OppslagResult = arbeidsforholdClient.finnArbeidsforholdForFnr(Fødselsnummer(fnr))
        when (arbeidsforhold) {
            is Success<*> -> call.respond(arbeidsforhold.data!!)
            is Failure -> call.respond(HttpStatusCode.InternalServerError, "Could not find arbeidsforhold")
        }
    }
}
