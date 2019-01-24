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
import no.nav.helse.http.aktør.*
import no.nav.helse.nais.*
import no.nav.helse.sts.*
import no.nav.helse.ws.*
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
private val authorizedUsers = listOf("srvspinne", "srvspa", "srvpleiepengesokna")

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
                log.info("authorization attempt for ${credentials.payload.subject}")
                if (credentials.payload.subject in authorizedUsers) {
                    log.info("authorization ok")
                    return@validate JWTPrincipal(credentials.payload)
                }
                log.info("authorization failed")
                return@validate null
            }
        }
    }

    val stsClient by lazy {
        stsClient(env.securityTokenServiceEndpointUrl,
                env.securityTokenUsername to env.securityTokenPassword
        )
    }

    val aktørregisterClient by lazy {
        AktørregisterClient(env.aktørregisterUrl, StsRestClient(
                env.stsRestUrl, env.securityTokenUsername, env.securityTokenPassword
        ))
    }

    routing {
        authenticate {
            inntekt{
                val port = Clients.InntektV3(env.inntektEndpointUrl)
                if (env.allowInsecureSoapRequests) {
                    stsClient.configureFor(port, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                } else {
                    stsClient.configureFor(port)
                }
                InntektClient(port)
            }
            person {
                val port = Clients.PersonV3(env.personEndpointUrl)
                port.apply {
                    if (env.allowInsecureSoapRequests) {
                        stsClient.configureFor(port, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                    } else {
                        stsClient.configureFor(port)
                    }
                }
                PersonClient(port)
            }
            arbeidsforhold(
                    clientFactory = {
                        val port = Clients.ArbeidsforholdV3(env.arbeidsforholdEndpointUrl)
                        if (env.allowInsecureSoapRequests) {
                            stsClient.configureFor(port, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                        } else {
                            stsClient.configureFor(port)
                        }
                        ArbeidsforholdClient(port)
                    },
                    aktørregisterClientFactory = { aktørregisterClient }
            )
            organisasjon {
                val port = Clients.OrganisasjonV5(env.organisasjonEndpointUrl)
                if (env.allowInsecureSoapRequests) {
                    stsClient.configureFor(port, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                } else {
                    stsClient.configureFor(port)
                }
                OrganisasjonClient(port)
            }

            sakOgBehandling{
                val port = Clients.SakOgBehandlingV1(env.sakOgBehandlingEndpointUrl)
                if (env.allowInsecureSoapRequests) {
                    stsClient.configureFor(port, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                } else {
                    stsClient.configureFor(port)
                }
                SakOgBehandlingClient(port)
            }
            sykepengeListe {
                val port = Clients.SykepengerV2(env.hentSykePengeListeEndpointUrl)
                if (env.allowInsecureSoapRequests) {
                    stsClient.configureFor(port, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                } else {
                    stsClient.configureFor(port)
                }
                SykepengerClient(port)
            }
            meldekort {
                val port = Clients.MeldekortUtbetalingsgrunnlagV1(env.meldekortEndpointUrl)
                if (env.allowInsecureSoapRequests) {
                    stsClient.configureFor(port, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
                } else {
                    stsClient.configureFor(port)
                }
                MeldekortClient(port)
            }
        }

        nais(collectorRegistry)
    }
}
