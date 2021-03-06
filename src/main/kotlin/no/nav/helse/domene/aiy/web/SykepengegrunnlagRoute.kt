package no.nav.helse.domene.aiy.web

import arrow.core.Either
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.util.pipeline.PipelineContext
import no.nav.helse.Feilårsak
import no.nav.helse.HttpFeil
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.aiy.SykepengegrunnlagService
import no.nav.helse.domene.aiy.domain.UtbetalingEllerTrekk
import no.nav.helse.domene.aiy.web.dto.UtbetalingerEllerTrekkResponse
import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjonsnummer
import no.nav.helse.respond
import no.nav.helse.respondFeil
import java.time.YearMonth
import java.time.format.DateTimeParseException

fun Route.sykepengegrunnlag(sykepengegrunnlagService: SykepengegrunnlagService) {

    get("api/inntekt/{aktorId}/beregningsgrunnlag/{virksomhetsnummer}") {
        hentInntekt { aktørId, fom, tom ->
            sykepengegrunnlagService.hentBeregningsgrunnlag(aktørId, Organisasjonsnummer(call.parameters["virksomhetsnummer"]!!), fom, tom)
        }
    }

    get("api/inntekt/{aktorId}/sammenligningsgrunnlag") {
        hentInntekt { aktørId, fom, tom ->
            sykepengegrunnlagService.hentSammenligningsgrunnlag(aktørId, fom, tom)
        }

    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.hentInntekt(f: (AktørId, YearMonth, YearMonth) -> Either<Feilårsak, List<UtbetalingEllerTrekk>>) {
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
            it.map(UtbetalingEllerTrekkDtoMapper::toDto)
        }.map {
            UtbetalingerEllerTrekkResponse(it)
        }.respond(call)
    }
}
