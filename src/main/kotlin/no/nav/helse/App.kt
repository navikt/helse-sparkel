package no.nav.helse

import com.auth0.jwk.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.prometheus.client.*
import io.prometheus.client.hotspot.*
import no.nav.helse.maksdato.*
import no.nav.helse.nais.*
import no.nav.helse.sts.*
import no.nav.helse.ws.*
import no.nav.helse.ws.arbeidsfordeling.*
import no.nav.helse.ws.arbeidsforhold.*
import no.nav.helse.ws.inntekt.*
import no.nav.helse.ws.meldekort.*
import no.nav.helse.ws.organisasjon.*
import no.nav.helse.ws.person.*
import no.nav.helse.ws.sakogbehandling.*
import no.nav.helse.ws.sts.*
import no.nav.helse.ws.sykepenger.*
import org.slf4j.event.*
import java.net.*
import java.util.*
import java.util.concurrent.*

private val collectorRegistry = CollectorRegistry.defaultRegistry
private val authorizedUsers = listOf("srvspinne", "srvspa", "srvpleiepengesokna", "srvpleiepenger-opp")

fun main() {
    val env = Environment()

    DefaultExports.initialize()

    val app = embeddedServer(Netty, 8080) {
        val jwkProvider = JwkProviderBuilder(URL(env.jwksUrl))
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build()

        sparkel(env, jwkProvider)
    }

    app.start(wait = false)

    Runtime.getRuntime().addShutdownHook(Thread {
        app.stop(5, 60, TimeUnit.SECONDS)
    })
}

fun Application.sparkel(env: Environment, jwkProvider: JwkProvider) {
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
                if (credentials.payload.subject in authorizedUsers) {
                    JWTPrincipal(credentials.payload)
                }
                else {
                    log.info("${credentials.payload.subject} is not authorized to use this app, denying access")
                    null
                }
            }
        }
    }

    val stsClientWs = stsClient(env.securityTokenServiceEndpointUrl,
            env.securityTokenUsername to env.securityTokenPassword)
    val stsClientRest = StsRestClient(
            env.stsRestUrl, env.securityTokenUsername, env.securityTokenPassword)
    val wsClients = WsClients(stsClientWs, stsClientRest, env.allowInsecureSoapRequests)

    routing {
        authenticate {
            inntekt(inntektClient = wsClients.inntekt(env.inntektEndpointUrl),
                    aktørregisterClient = wsClients.aktør(env.aktørregisterUrl)
            )

            arbeidsfordeling(ArbeidsfordelingService(
                        arbeidsfordelingClient = wsClients.arbeidsfordeling(env.arbeidsfordelingEndpointUrl),
                        personClient = wsClients.person(env.personEndpointUrl))
            )

            person(wsClients.person(env.personEndpointUrl))

            arbeidsforhold(
                    arbeidsforholdClient = wsClients.arbeidsforhold(env.arbeidsforholdEndpointUrl),
                    aktørregisterClient = wsClients.aktør(env.aktørregisterUrl),
                    organisasjonsClient = wsClients.organisasjon(env.organisasjonEndpointUrl)
            )

            organisasjon(wsClients.organisasjon(env.organisasjonEndpointUrl))

            sakOgBehandling(wsClients.sakOgBehandling(env.sakOgBehandlingEndpointUrl))

            sykepengeListe(
                sykepenger = wsClients.sykepengeliste(env.hentSykePengeListeEndpointUrl),
                aktørregisterClient = wsClients.aktør(env.aktørregisterUrl)
            )

            meldekort(wsClients.meldekort(env.meldekortEndpointUrl))

            maksdato("http://maksdato")
        }

        nais(collectorRegistry)
    }
}
