package no.nav.helse

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.ws.Clients
import no.nav.helse.ws.arbeidsforhold.arbeidsforhold
import no.nav.helse.ws.inntekt.inntekt
import no.nav.helse.ws.organisasjon.organisasjon
import no.nav.helse.ws.person.person
import no.nav.helse.ws.sakogbehandling.sakOgBehandling
import org.slf4j.LoggerFactory
import java.util.Collections.emptySet
import java.util.concurrent.TimeUnit

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

private val log = LoggerFactory.getLogger("App")
private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry

data class Environment(val securityTokenServiceEndpointUrl: String = getEnvVar("SECURITY_TOKEN_SERVICE_URL"),
                       val securityTokenUsername: String = getEnvVar("SECURITY_TOKEN_SERVICE_USERNAME"),
                       val securityTokenPassword: String = getEnvVar("SECURITY_TOKEN_SERVICE_PASSWORD"),
                       val personEndpointUrl: String = getEnvVar("PERSON_ENDPOINTURL"),
                       val inntektEndpointUrl: String = getEnvVar("INNTEKT_ENDPOINTURL"),
                       val arbeidsforholdEndpointUrl: String = getEnvVar("AAREG_ENDPOINTURL"),
                       val organisasjonEndpointUrl: String = getEnvVar("ORGANISASJON_ENDPOINTURL"),
                       val sakOgBehandlingEndpointUrl: String = getEnvVar("SAK_OG_BEHANDLING_ENDPOINTURL"))

fun main() {
    val env = Environment()

    val app = App(env)

    app.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        app.stop()
    })
}

class App(env: Environment = Environment()) {
    private val nettyServer: NettyApplicationEngine
    private val clients: Clients = Clients(env)

    init {
        DefaultExports.initialize()

        nettyServer = embeddedServer(Netty, 8080) {
            routing {
                inntekt(clients.inntektClient)
                person(clients.personClient)
                arbeidsforhold(clients.arbeidsforholdClient)
                organisasjon(clients.organisasjonClient)
                sakOgBehandling(clients.sakOgBehandlingClient)

                get("/isalive") {
                    call.respondText("ALIVE", ContentType.Text.Plain)
                }

                get("/isready") {
                    call.respondText("READY", ContentType.Text.Plain)
                }

                get("/metrics") {
                    val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()
                    call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                        TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
                    }
                }
            }
        }
    }

    fun start() {
        nettyServer.start(wait = false)
    }

    fun stop() {
        nettyServer.stop(5, 60, TimeUnit.SECONDS)
    }
}
