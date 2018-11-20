package no.nav.helse

import io.grpc.Server
import io.grpc.ServerBuilder
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
import no.nav.helse.ws.EndpointSTSClientConfig
import no.nav.helse.ws.WsClientBuilder
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdGrpc
import no.nav.helse.ws.arbeidsforhold.arbeidsforhold
import no.nav.helse.ws.inntekt.InntektClient
import no.nav.helse.ws.inntekt.inntekt
import no.nav.helse.ws.organisasjon.OrganisasjonClient
import no.nav.helse.ws.organisasjon.organisasjon
import no.nav.helse.ws.person.PersonClient
import no.nav.helse.ws.person.person
import no.nav.helse.ws.sakogbehandling.SakOgBehandlingClient
import no.nav.helse.ws.sakogbehandling.sakOgBehandling
import no.nav.helse.ws.sts.STSClientBuilder
import no.nav.helse.ws.sts.STSProperties
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1
import org.slf4j.LoggerFactory
import java.net.URI
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
                       val sakOgBehandlingEndpointUrl: String = getEnvVar("SAK_OG_BEHANDLING_ENDPOINTURL"));

fun main() {
    val env = Environment()

    App(env).start()
}

class App(val env: Environment = Environment()) {
    private val nettyServer: NettyApplicationEngine
    private val grpcServer: Server

    init {
        DefaultExports.initialize()

        val wsClientBuilder = WsClientBuilder()
        val stsClient = STSClientBuilder().build(STSProperties(
                URI(env.securityTokenServiceEndpointUrl),
                env.securityTokenUsername,
                env.securityTokenPassword
        ))
        val endpointSTSClientConfig = EndpointSTSClientConfig(stsClient)

        val personV3 = wsClientBuilder.createPort(env.personEndpointUrl, PersonV3::class.java)
        endpointSTSClientConfig.configureRequestSamlToken(personV3, EndpointSTSClientConfig.STS_SAML_POLICY)
        val personClient = PersonClient(personV3)

        val inntektV3 = wsClientBuilder.createPort(env.inntektEndpointUrl, InntektV3::class.java)
        endpointSTSClientConfig.configureRequestSamlToken(inntektV3, EndpointSTSClientConfig.STS_SAML_POLICY)
        val inntektClient = InntektClient(inntektV3)

        val arbeidsforholdV3: ArbeidsforholdV3 = wsClientBuilder.createPort(env.arbeidsforholdEndpointUrl, ArbeidsforholdV3::class.java)
        endpointSTSClientConfig.configureRequestSamlToken(arbeidsforholdV3, EndpointSTSClientConfig.STS_SAML_POLICY)
        val arbeidsforholdClient = ArbeidsforholdClient(arbeidsforholdV3)

        val organisasjonV5: OrganisasjonV5 = wsClientBuilder.createPort(env.organisasjonEndpointUrl, OrganisasjonV5::class.java)
        endpointSTSClientConfig.configureRequestSamlToken(organisasjonV5, EndpointSTSClientConfig.STS_SAML_POLICY)
        val organisasjonClient = OrganisasjonClient(organisasjonV5)

        val sakOgBehandlingV1: SakOgBehandlingV1 = wsClientBuilder.createPort(env.sakOgBehandlingEndpointUrl, SakOgBehandlingV1::class.java)
        endpointSTSClientConfig.configureRequestSamlToken(sakOgBehandlingV1, EndpointSTSClientConfig.STS_SAML_POLICY)
        val sakOgBehandlingClient = SakOgBehandlingClient(sakOgBehandlingV1)

        nettyServer = embeddedServer(Netty, 8080) {
            routing {
                inntekt(inntektClient)
                person(personClient)
                arbeidsforhold(arbeidsforholdClient)
                organisasjon(organisasjonClient)
                sakOgBehandling(sakOgBehandlingClient)

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

        grpcServer = ServerBuilder.forPort(8081)
            .addService(ArbeidsforholdGrpc(arbeidsforholdClient))
            .build()
    }

    fun start() {
        nettyServer.start(wait = false)
        grpcServer.start()
    }

    fun stop() {
        grpcServer.shutdownNow()
        nettyServer.stop(5, 60, TimeUnit.SECONDS)
        grpcServer.awaitTermination(5, TimeUnit.SECONDS)
    }
}
