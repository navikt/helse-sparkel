package no.nav.helse

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
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
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.nais.nais
import no.nav.helse.ws.Clients
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.ws.arbeidsforhold.arbeidsforhold
import no.nav.helse.ws.inntekt.InntektClient
import no.nav.helse.ws.inntekt.inntekt
import no.nav.helse.ws.meldekort.MeldekortClient
import no.nav.helse.ws.meldekort.meldekort
import no.nav.helse.ws.organisasjon.OrganisasjonClient
import no.nav.helse.ws.organisasjon.organisasjon
import no.nav.helse.ws.person.PersonClient
import no.nav.helse.ws.person.person
import no.nav.helse.ws.sakogbehandling.SakOgBehandlingClient
import no.nav.helse.ws.sakogbehandling.sakOgBehandling
import no.nav.helse.ws.sts.STS_SAML_POLICY_NO_TRANSPORT_BINDING
import no.nav.helse.ws.sts.configureFor
import no.nav.helse.ws.sts.stsClient
import no.nav.helse.ws.sykepenger.SykepengerClient
import no.nav.helse.ws.sykepenger.sykepengeListe
import org.slf4j.event.Level
import redis.clients.jedis.Jedis
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

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

    val cache  by lazy { RedisIdentCache(Jedis(env.redisHost, Integer.valueOf(env.redisPort))) }
    val identLookup by lazy {
        IdentLookup({
            AktørregisterClient(env.aktørregisterUrl, StsRestClient(
                    env.stsRestUrl, env.securityTokenUsername, env.securityTokenPassword
            ))
        }, cache)
    }

    routing {
        authenticate {
            identMapping{
                identLookup
            }
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
                    identFactory = { identLookup }
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
