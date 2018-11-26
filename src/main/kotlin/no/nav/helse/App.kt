package no.nav.helse

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
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

    val jwkProvider = JwkProviderBuilder(env.jwksUrl)
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    init {
        DefaultExports.initialize()

        nettyServer = embeddedServer(Netty, 8080) {

            install(CallLogging)

            install(Authentication) {
                jwt {
                    verifier(jwkProvider, env.jwtIssuer)
                    realm = "Helse Sparkel"
                    validate { credentials ->
                        if (credentials.payload.subject in listOf("srvspinne", "srvsplitt")) {
                            JWTPrincipal(credentials.payload)
                        }
                        null
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
