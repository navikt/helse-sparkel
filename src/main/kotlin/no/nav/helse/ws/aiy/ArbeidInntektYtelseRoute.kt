package no.nav.helse.ws.aiy

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.HttpFeil
import no.nav.helse.map
import no.nav.helse.respond
import no.nav.helse.respondFeil
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.aiy.domain.ArbeidsforholdMedInntekt
import no.nav.helse.ws.aiy.domain.InntektUtenArbeidsgiver
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdDTO
import no.nav.helse.ws.arbeidsforhold.ArbeidsgiverDTO
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsgiver
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun Route.arbeidInntektYtelse(
        arbeidInntektYtelseService: ArbeidInntektYtelseService
) {
    get("api/arbeidsforhold/{aktorId}/inntekter") {
        if (!call.request.queryParameters.contains("fom") || !call.request.queryParameters.contains("tom")) {
            call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "you need to supply query parameter fom and tom"))
        } else {
            val fom = try {
                LocalDate.parse(call.request.queryParameters["fom"]!!)
            } catch (err: DateTimeParseException) {
                call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "fom must be specified as yyyy-mm-dd"))
                return@get
            }
            val tom = try {
                LocalDate.parse(call.request.queryParameters["tom"]!!)
            } catch (err: DateTimeParseException) {
                call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "tom must be specified as yyyy-mm-dd"))
                return@get
            }

            arbeidInntektYtelseService.finnArbeidsforholdMedInntekter(AktørId(call.parameters["aktorId"]!!), fom, tom).map {
                it.map {
                    ArbeidsforholdMedInntektDTO(
                            ArbeidsforholdDTO(ArbeidsgiverDTO((it.arbeidsforhold.arbeidsgiver as Arbeidsgiver.Virksomhet).virksomhet.orgnr.value, it.arbeidsforhold.arbeidsgiver.virksomhet.navn), it.arbeidsforhold.startdato, it.arbeidsforhold.sluttdato),
                            it.inntekter)
                }
            }.map {
                ArbeidsforholdMedInntekterResponse(it)
            }.respond(call)
        }
    }
}

data class ArbeidsforholdMedInntektDTO(val arbeidsforhold: ArbeidsforholdDTO, val inntekter: List<InntektUtenArbeidsgiver>)
data class ArbeidsforholdMedInntekterResponse(val arbeidsforhold: List<ArbeidsforholdMedInntektDTO>)
