package no.nav.helse.ws.inntekt

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.util.pipeline.PipelineContext
import no.nav.helse.Either
import no.nav.helse.Feilårsak
import no.nav.helse.HttpFeil
import no.nav.helse.map
import no.nav.helse.respond
import no.nav.helse.respondFeil
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.inntekt.domain.Inntekt
import no.nav.helse.ws.inntekt.domain.Opptjeningsperiode
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeParseException

fun Route.inntekt(inntektService: InntektService) {

    get("api/inntekt/{aktorId}/beregningsgrunnlag") {
        hentInntekt { aktørId, fom, tom ->
            inntektService.hentBeregningsgrunnlag(aktørId, fom, tom)
        }
    }

    get("api/inntekt/{aktorId}/sammenligningsgrunnlag") {
        hentInntekt { aktørId, fom, tom ->
            inntektService.hentSammenligningsgrunnlag(aktørId, fom, tom)
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.hentInntekt(f: (AktørId, YearMonth, YearMonth) -> Either<Feilårsak, List<Inntekt>>) {
    if (!call.request.queryParameters.contains("fom") || !call.request.queryParameters.contains("tom")) {
        call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "you need to supply query parameter fom and tom"))
    } else {
        val fom = try {
            YearMonth.parse(call.request.queryParameters["fom"]!!)
        } catch (err: DateTimeParseException) {
            call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "fom must be specified as yyyy-mm"))
            return
        }
        val tom = try {
            YearMonth.parse(call.request.queryParameters["tom"]!!)
        } catch (err: DateTimeParseException) {
            call.respondFeil(HttpFeil(HttpStatusCode.BadRequest, "tom must be specified as yyyy-mm"))
            return
        }

        f(AktørId(call.parameters["aktorId"]!!), fom, tom).map {
            it.map {
                InntektDTO(ArbeidsgiverDTO(it.virksomhet.identifikator), Opptjeningsperiode(it.utbetalingsperiode.atDay(1), it.utbetalingsperiode.atEndOfMonth()), it.beløp)
            }
        }.map {
            InntektResponse(it)
        }.respond(call)
    }
}

data class ArbeidsgiverDTO(val orgnr: String)
data class InntektDTO(val arbeidsgiver: ArbeidsgiverDTO, val opptjeningsperiode: Opptjeningsperiode, val beløp: BigDecimal)
data class InntektResponse(val inntekter: List<InntektDTO>)
