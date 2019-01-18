package no.nav.helse.ws.arbeidsforhold

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.*
import no.nav.helse.ws.Fødselsnummer

private const val UUID_QUERY_PARAMETER = "uuid"

fun Route.arbeidsforhold(
        clientFactory: () -> ArbeidsforholdClient,
        identFactory: () -> IdentLookup
) {
    val arbeidsforholdClient by lazy(clientFactory)
    val identLookup by lazy(identFactory)

    fun getFnrFromUuid(uuid : String) : Fødselsnummer? {
        try {
            val idents = identLookup.fromUUID(uuid)
            for (ident in idents) {
                if (IdentType.NorskIdent == ident.type) {
                    return Fødselsnummer(ident.ident)
                }
            }
            return null
        } catch (cause : IllegalArgumentException) {
            return null
        }

    }

    get("api/arbeidsforhold") {
        if (!call.request.queryParameters.contains(UUID_QUERY_PARAMETER)) {
            call.respond(HttpStatusCode.BadRequest, "you need to supply query parameter $UUID_QUERY_PARAMETER")
        } else {
            val uuid = call.request.queryParameters[UUID_QUERY_PARAMETER]!!
            val fnr = getFnrFromUuid(uuid)
            if (fnr == null) {
                call.respond(HttpStatusCode.NotFound, "No match on uuid '$uuid'")
            } else {
                val lookupResult: OppslagResult = arbeidsforholdClient.finnArbeidsforholdForFnr(fnr)
                when (lookupResult) {
                    is Success<*> -> call.respond(lookupResult.data!!)
                    is Failure -> call.respond(HttpStatusCode.InternalServerError, "that didn't go so well...")
                }
            }
        }
    }
}

