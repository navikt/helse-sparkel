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
import io.ktor.request.path
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.http.aktør.AktørregisterService
import no.nav.helse.maksdato.maksdato
import no.nav.helse.nais.nais
import no.nav.helse.sts.StsRestClient
import no.nav.helse.ws.WsClients
import no.nav.helse.ws.arbeidsfordeling.ArbeidsfordelingService
import no.nav.helse.ws.arbeidsfordeling.arbeidsfordeling
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdService
import no.nav.helse.ws.arbeidsforhold.arbeidsforhold
import no.nav.helse.ws.inntekt.InntektService
import no.nav.helse.ws.inntekt.inntekt
import no.nav.helse.ws.meldekort.MeldekortService
import no.nav.helse.ws.meldekort.meldekort
import no.nav.helse.ws.organisasjon.OrganisasjonService
import no.nav.helse.ws.organisasjon.organisasjon
import no.nav.helse.ws.person.PersonService
import no.nav.helse.ws.person.person
import no.nav.helse.ws.sakogbehandling.SakOgBehandlingService
import no.nav.helse.ws.sakogbehandling.sakOgBehandling
import no.nav.helse.ws.sts.stsClient
import no.nav.helse.ws.sykepenger.SykepengelisteService
import no.nav.helse.ws.sykepenger.sykepengeListe
import org.slf4j.event.Level
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

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

        val stsClientWs = stsClient(env.securityTokenServiceEndpointUrl,
                env.securityTokenUsername to env.securityTokenPassword, env.disableCNCheck)

        val stsClientRest = StsRestClient(
                env.stsRestUrl, env.securityTokenUsername, env.securityTokenPassword)

        val wsClients = WsClients(stsClientWs, stsClientRest, env.allowInsecureSoapRequests)

        val inntektService = InntektService(wsClients.inntekt(env.inntektEndpointUrl))

        val personService = PersonService(wsClients.person(env.personEndpointUrl))

        val arbeidsfordelingService = ArbeidsfordelingService(
                arbeidsfordelingClient = wsClients.arbeidsfordeling(env.arbeidsfordelingEndpointUrl),
                personService = personService)

        val organisasjonService = OrganisasjonService(wsClients.organisasjon(env.organisasjonEndpointUrl))

        val arbeidsforholdService = ArbeidsforholdService(
                arbeidsforholdClient = wsClients.arbeidsforhold(env.arbeidsforholdEndpointUrl),
                organisasjonService = organisasjonService
        )

        val sakOgBehandlingService = SakOgBehandlingService(wsClients.sakOgBehandling(env.sakOgBehandlingEndpointUrl))

        val sykepengelisteService = SykepengelisteService(
                sykepengerClient = wsClients.sykepengeliste(env.hentSykePengeListeEndpointUrl),
                aktørregisterService = AktørregisterService(wsClients.aktør(env.aktørregisterUrl))
        )

        val meldekortServie = MeldekortService(wsClients.meldekort(env.meldekortEndpointUrl))

        sparkel(env.jwtIssuer, jwkProvider, arbeidsfordelingService, arbeidsforholdService, inntektService, meldekortServie,
                organisasjonService, personService, sakOgBehandlingService, sykepengelisteService)
    }

    app.start(wait = false)

    Runtime.getRuntime().addShutdownHook(Thread {
        app.stop(5, 60, TimeUnit.SECONDS)
    })
}

fun Application.sparkel(
        jwtIssuer: String,
        jwkProvider: JwkProvider,
        arbeidsfordelingService: ArbeidsfordelingService,
        arbeidsforholdService: ArbeidsforholdService,
        inntektService: InntektService,
        meldekortService: MeldekortService,
        organisasjonService: OrganisasjonService,
        personService: PersonService,
        sakOgBehandlingService: SakOgBehandlingService,
        sykepengelisteService: SykepengelisteService
) {
    install(CallId) {
        header("Nav-Call-Id")

        generate { UUID.randomUUID().toString() }
    }

    install(CallLogging) {
        level = Level.INFO
        callIdMdc("call_id")
        filter {
            it.request.path() != "/isready"
                    && it.request.path() != "/isalive"
                    && it.request.path() != "/metrics"
        }
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JsonContentConverter())
    }

    install(Authentication) {
        jwt {
            verifier(jwkProvider, jwtIssuer)
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

    routing {
        authenticate {

            inntekt(inntektService)

            arbeidsfordeling(arbeidsfordelingService)

            person(personService)

            arbeidsforhold(arbeidsforholdService)

            organisasjon(organisasjonService)

            sakOgBehandling(sakOgBehandlingService)

            sykepengeListe(sykepengelisteService)

            meldekort(meldekortService)

            maksdato("http://maksdato")
        }

        nais(collectorRegistry)
    }
}
