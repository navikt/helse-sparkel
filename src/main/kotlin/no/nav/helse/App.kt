package no.nav.helse

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallId
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.callIdMdc
import io.ktor.http.ContentType
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.nais.nais
import no.nav.helse.ws.Clients
import no.nav.helse.ws.arbeidsforhold.arbeidsforhold
import no.nav.helse.ws.inntekt.inntekt
import no.nav.helse.ws.organisasjon.organisasjon
import no.nav.helse.ws.person.person
import no.nav.helse.ws.sakogbehandling.sakOgBehandling
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry

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

    private val log = LoggerFactory.getLogger("App")

    val jwkProvider = JwkProviderBuilder(URL(env.jwksUrl))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    init {
        DefaultExports.initialize()

        nettyServer = embeddedServer(Netty, 8080) {
            install(CallId) {
                header("Nav-Call-Id")

                generate { UUID.randomUUID().toString() }
            }

            install(CallLogging) {
                level = Level.INFO
                callIdMdc("call_id")
            }

            install(ContentNegotiation) {
                register(ContentType.Application.Json, JsonContentConverter())
            }

            install(Authentication) {
                jwt {
                    verifier(jwkProvider, env.jwtIssuer)
                    realm = "Helse Sparkel"
                    validate { credentials ->
                        log.info("authorization attempt for ${credentials.payload.subject}")
                        if (credentials.payload.subject in listOf("srvspinne", "srvsplitt")) {
                            log.info("authorization ok")
                            return@validate JWTPrincipal(credentials.payload)
                        }
                        log.info("authorization failed")
                        return@validate null
                    }
                }
            }

            routing {
                authenticate {
                    inntekt(clients.inntektClient)
                    person(clients.personClient)
                    arbeidsforhold(clients.arbeidsforholdClient)
                    organisasjon(clients.organisasjonClient)
                    sakOgBehandling(clients.sakOgBehandlingClient)
                }

                nais(collectorRegistry)
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
