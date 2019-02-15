package no.nav.helse.ws.arbeidsforhold

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.*
import no.nav.helse.http.aktør.*
import no.nav.helse.ws.Fødselsnummer
import no.nav.helse.ws.organisasjon.OrganisasjonClient
import no.nav.helse.ws.organisasjon.OrganisasjonResponse
import no.nav.helse.ws.organisasjon.OrganisasjonsAttributt
import no.nav.helse.ws.organisasjon.OrganisasjonsNummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon
import java.time.LocalDate

fun Route.arbeidsforhold(
        clientFactory: () -> ArbeidsforholdClient,
        aktørregisterClientFactory: () -> AktørregisterClient,
        organisasjonsClientFactory: () -> OrganisasjonClient
) {
    val arbeidsforholdClient by lazy(clientFactory)
    val aktørregisterClient by lazy(aktørregisterClientFactory)
    val organisasjonsClient by lazy(organisasjonsClientFactory)

    get("api/arbeidsforhold/{aktorId}") {
        if (!call.request.queryParameters.contains("fom") || !call.request.queryParameters.contains("tom")) {
            call.respond(HttpStatusCode.BadRequest, "you need to supply query parameter fom and tom")
        } else {
            val fom = LocalDate.parse(call.request.queryParameters["fom"]!!)
            val tom = LocalDate.parse(call.request.queryParameters["tom"]!!)

            val fnr = Fødselsnummer(aktørregisterClient.gjeldendeNorskIdent(call.parameters["aktorId"]!!))

            val lookupResult: OppslagResult = arbeidsforholdClient.finnArbeidsforhold(fnr, fom, tom)
            when (lookupResult) {
                is Success<*> -> call.respond(genererResponse(arbeidsforholdListe = lookupResult.data as List<Arbeidsforhold>, organisasjonsClient = organisasjonsClient))
                is Failure -> call.respond(HttpStatusCode.InternalServerError, "that didn't go so well...")
            }
        }
    }
}

private fun genererResponse(arbeidsforholdListe : List<Arbeidsforhold>, organisasjonsClient: OrganisasjonClient) : ArbeidsforholdResponse {
    val organisasjoner = mutableListOf<OrganisasjonArbeidsforhold>()
    arbeidsforholdListe.filter { it.arbeidsgiver is Organisasjon }.forEach { arbeidsforhold ->
        val organisasjonsnummer = (arbeidsforhold.arbeidsgiver as Organisasjon).orgnummer
        val navn : String? = hentOrganisasjonsNavn(organisasjonsClient, arbeidsforhold.arbeidsgiver as Organisasjon)
        organisasjoner.add(OrganisasjonArbeidsforhold(organisasjonsnummer = organisasjonsnummer, navn = navn))

    }
    return ArbeidsforholdResponse(organisasjoner = organisasjoner.toList())
}

private fun hentOrganisasjonsNavn(organisasjonsClient: OrganisasjonClient, organisasjon: Organisasjon) : String? {
    return if (organisasjon.navn.isNullOrBlank()) {
        val oppslagResult  = organisasjonsClient.hentOrganisasjon(
                orgnr = OrganisasjonsNummer(organisasjon.orgnummer),
                attributter = listOf(OrganisasjonsAttributt("navn"))
        )
        if (oppslagResult is Success<*>) (oppslagResult.data as OrganisasjonResponse).navn else null
    } else {
        organisasjon.navn
    }
}

data class ArbeidsforholdResponse(val organisasjoner: List<OrganisasjonArbeidsforhold>)
data class OrganisasjonArbeidsforhold(val organisasjonsnummer: String, val navn: String?)
