package no.nav.helse

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.ws.EndpointSTSClientConfig
import no.nav.helse.ws.WsClientBuilder
import no.nav.helse.ws.inntekt.InntektClient
import no.nav.helse.ws.inntekt.inntekt
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.ws.arbeidsforhold.arbeidsforhold
import no.nav.helse.ws.person.PersonClient
import no.nav.helse.ws.person.person
import no.nav.helse.ws.sts.STSClientBuilder
import no.nav.helse.ws.sts.STSProperties
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.Collections.emptySet

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

private val wsClientBuilder = WsClientBuilder()
private val stsClient = STSClientBuilder().build(STSProperties(
        URI(getEnvVar("SECURITY_TOKEN_SERVICE_URL")),
        getEnvVar("SECURITY_TOKEN_SERVICE_USERNAME"),
        getEnvVar("SECURITY_TOKEN_SERVICE_PASSWORD")
))
private val endpointSTSClientConfig = EndpointSTSClientConfig(stsClient)

private val log = LoggerFactory.getLogger("App")
private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry

fun main() {
    DefaultExports.initialize()

    val personV3 = wsClientBuilder.createPort(getEnvVar("PERSON_ENDPOINTURL"), PersonV3::class.java)
    endpointSTSClientConfig.configureRequestSamlToken(personV3, EndpointSTSClientConfig.STS_SAML_POLICY)
    val personClient = PersonClient(personV3)

    val inntektV3 = wsClientBuilder.createPort(getEnvVar("INNTEKT_ENDPOINTURL"), InntektV3::class.java)
    endpointSTSClientConfig.configureRequestSamlToken(inntektV3, EndpointSTSClientConfig.STS_SAML_POLICY)
    val inntektClient = InntektClient(inntektV3)

    val arbeidsforholdV3: ArbeidsforholdV3 = wsClientBuilder.createPort(getEnvVar("AAREG_ENDPOINTURL"), ArbeidsforholdV3::class.java)
    endpointSTSClientConfig.configureRequestSamlToken(arbeidsforholdV3, EndpointSTSClientConfig.STS_SAML_POLICY)
    val arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3)

    embeddedServer(Netty, 8080) {
        routing {
            inntekt(inntektClient)
            person(personClient)
            arbeidsforhold(arbeidsforholdClient)

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
    }.start(wait = false)
}
