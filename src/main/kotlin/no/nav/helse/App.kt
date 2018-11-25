package no.nav.helse

import com.auth0.jwk.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.prometheus.client.*
import io.prometheus.client.hotspot.*
import no.nav.helse.nais.*
import no.nav.helse.ws.*
import no.nav.helse.ws.arbeidsforhold.*
import no.nav.helse.ws.inntekt.*
import no.nav.helse.ws.organisasjon.*
import no.nav.helse.ws.person.*
import no.nav.helse.ws.sakogbehandling.*
import java.util.concurrent.*

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

    val jwkProvider = JwkProviderBuilder(env.jwtIssuer)
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
                        if (credentials.payload.audience.contains(env.jwtAudience)) JWTPrincipal(credentials.payload) else null
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
