package no.nav.helse.ws.arbeidsforhold

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.OppslagResult
import no.nav.helse.http.aktør.AktørregisterClient
import no.nav.helse.map
import no.nav.helse.ws.Fødselsnummer
import no.nav.helse.ws.organisasjon.OrganisasjonClient
import no.nav.helse.ws.organisasjon.OrganisasjonsAttributt
import no.nav.helse.ws.organisasjon.OrganisasjonsNummer
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

            val lookupResult = arbeidsforholdClient.finnArbeidsforhold(fnr, fom, tom)

            when (lookupResult) {
                is OppslagResult.Ok -> {
                    val listeAvArbeidsgivere = lookupResult.data.map {
                        it.arbeidsgiver
                    }.filter {
                        it is Organisasjon
                    }.map {
                        it as Organisasjon
                    }.map { organisasjon ->
                        if (organisasjon.navn.isNullOrBlank()) {
                            organisasjonsClient.hentOrganisasjon(
                                    orgnr = OrganisasjonsNummer(organisasjon.orgnummer),
                                    attributter = listOf(OrganisasjonsAttributt("navn"))
                            ).map { organisasjonResponse ->
                                Organisasjon().apply {
                                    navn = organisasjonResponse.navn
                                    orgnummer = organisasjon.orgnummer
                                }
                            }
                        } else {
                            OppslagResult.Ok(organisasjon)
                        }
                    }.let {
                        OppslagResult.Ok(it.map { oppslagResultat ->
                            when (oppslagResultat) {
                                is OppslagResult.Ok -> oppslagResultat.data
                                is OppslagResult.Feil -> {
                                    return@let oppslagResultat.copy()
                                }
                            }
                        }.map { organisasjon ->
                            OrganisasjonArbeidsforhold(organisasjon.orgnummer, organisasjon.navn)
                        })
                    }

                    when (listeAvArbeidsgivere) {
                        is OppslagResult.Ok -> call.respond(ArbeidsforholdResponse(listeAvArbeidsgivere.data))
                        is OppslagResult.Feil -> call.respond(listeAvArbeidsgivere.httpCode, listeAvArbeidsgivere.feil)
                    }
                }
                is OppslagResult.Feil -> call.respond(lookupResult.httpCode, lookupResult.feil)
            }
        }
    }
}

data class ArbeidsforholdResponse(val organisasjoner: List<OrganisasjonArbeidsforhold>)
data class OrganisasjonArbeidsforhold(val organisasjonsnummer: String, val navn: String?)
