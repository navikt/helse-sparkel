package no.nav.helse.nais

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.prometheus.client.*
import io.prometheus.client.exporter.common.*
import java.util.*

fun Routing.nais(collectorRegistry: CollectorRegistry) {
    get("/isalive") {
        call.respondText("ALIVE", ContentType.Text.Plain)
    }

    get("/isready") {
        call.respondText("READY", ContentType.Text.Plain)
    }

    get("/metrics") {
        val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: Collections.emptySet()
        call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
            TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
        }
    }
}