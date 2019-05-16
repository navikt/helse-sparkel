package no.nav.helse

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallId
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.callIdMdc
import io.ktor.http.ContentType
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.uri
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.domene.aiy.ArbeidInntektYtelseService
import no.nav.helse.domene.aiy.arbeidInntektYtelse
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.aktør.fnrForAktør
import no.nav.helse.domene.arbeid.ArbeidsforholdService
import no.nav.helse.domene.arbeid.ArbeidsgiverService
import no.nav.helse.domene.arbeid.arbeidsforhold
import no.nav.helse.domene.arbeidsfordeling.ArbeidsfordelingService
import no.nav.helse.domene.arbeidsfordeling.arbeidsfordeling
import no.nav.helse.domene.infotrygd.InfotrygdBeregningsgrunnlagService
import no.nav.helse.domene.infotrygd.infotrygdBeregningsgrunnlag
import no.nav.helse.domene.organisasjon.OrganisasjonService
import no.nav.helse.domene.organisasjon.organisasjon
import no.nav.helse.domene.person.PersonService
import no.nav.helse.domene.person.person
import no.nav.helse.domene.sykepengegrunnlag.SykepengegrunnlagService
import no.nav.helse.domene.sykepengegrunnlag.sykepengegrunnlag
import no.nav.helse.domene.sykepengehistorikk.SykepengehistorikkService
import no.nav.helse.domene.sykepengehistorikk.sykepengehistorikk
import no.nav.helse.domene.utbetaling.UtbetalingOgTrekkService
import no.nav.helse.domene.ytelse.YtelseService
import no.nav.helse.domene.ytelse.ytelse
import no.nav.helse.nais.nais
import no.nav.helse.oppslag.WsClients
import no.nav.helse.oppslag.sts.stsClient
import no.nav.helse.probe.DatakvalitetProbe
import no.nav.helse.probe.SensuClient
import no.nav.helse.sts.StsRestClient
import org.slf4j.event.Level
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

private val collectorRegistry = CollectorRegistry.defaultRegistry
private val authorizedUsers = listOf("srvspinne", "srvspa", "srvpleiepengesokna", "srvpleiepenger-opp", "srvspinder", "srvspenn")

fun main() {
    val env = Environment()

    DefaultExports.initialize()

    val app = embeddedServer(Netty, 8080) {
        val jwkProvider = JwkProviderBuilder(URL(env.jwksUrl))
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build()

        val stsClientWs = stsClient(env.securityTokenServiceEndpointUrl,
                env.securityTokenUsername to env.securityTokenPassword)

        val stsClientRest = StsRestClient(
                env.stsRestUrl, env.securityTokenUsername, env.securityTokenPassword)

        val wsClients = WsClients(stsClientWs, stsClientRest)

        val organisasjonService = OrganisasjonService(wsClients.organisasjon(env.organisasjonEndpointUrl))

        val sensuClient = SensuClient("sensu.nais", 3030)
        val datakvalitetProbe = DatakvalitetProbe(sensuClient, organisasjonService)

        val inntektClient = wsClients.inntekt(env.inntektEndpointUrl)
        val inntektService = UtbetalingOgTrekkService(inntektClient, datakvalitetProbe)

        val personService = PersonService(wsClients.person(env.personEndpointUrl))

        val arbeidsfordelingService = ArbeidsfordelingService(
                arbeidsfordelingClient = wsClients.arbeidsfordeling(env.arbeidsfordelingEndpointUrl),
                personService = personService)

        val arbeidsforholdClient = wsClients.arbeidsforhold(env.arbeidsforholdEndpointUrl)

        val arbeidsforholdService = ArbeidsforholdService(
                arbeidsforholdClient = arbeidsforholdClient,
                inntektClient = inntektClient,
                datakvalitetProbe = datakvalitetProbe
        )

        val arbeidsgiverService = ArbeidsgiverService(
                arbeidsforholdClient = arbeidsforholdClient,
                organisasjonService = organisasjonService
        )

        val arbeidsforholdMedInntektService = ArbeidInntektYtelseService(
                arbeidsforholdService = arbeidsforholdService,
                utbetalingOgTrekkService = inntektService,
                organisasjonService = organisasjonService,
                datakvalitetProbe = datakvalitetProbe
        )

        val aktørregisterService = AktørregisterService(wsClients.aktør(env.aktørregisterUrl))

        val sykepengegrunnlagService = SykepengegrunnlagService(inntektService, organisasjonService)

        val infotrygdBeregningsgrunnlagService = InfotrygdBeregningsgrunnlagService(
                infotrygdClient = wsClients.infotrygdBeregningsgrunnlag(env.finnInfotrygdGrunnlagListeEndpointUrl),
                aktørregisterService = aktørregisterService
        )

        val sykepengehistorikkService = SykepengehistorikkService(infotrygdBeregningsgrunnlagService)

        val ytelseService = YtelseService(infotrygdBeregningsgrunnlagService, wsClients.meldekortUtbetalingsgrunnlag(env.meldekortEndpointUrl))

        sparkel(
                env.jwtIssuer,
                jwkProvider,
                arbeidsfordelingService,
                arbeidsforholdService,
                arbeidsgiverService,
                arbeidsforholdMedInntektService,
                organisasjonService,
                personService,
                sykepengegrunnlagService,
                infotrygdBeregningsgrunnlagService,
                aktørregisterService,
                sykepengehistorikkService,
                ytelseService
        )
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
        arbeidsgiverService: ArbeidsgiverService,
        arbeidInntektYtelseService: ArbeidInntektYtelseService,
        organisasjonService: OrganisasjonService,
        personService: PersonService,
        sykepengegrunnlagService: SykepengegrunnlagService,
        infotrygdBeregningsgrunnlagService: InfotrygdBeregningsgrunnlagService,
        aktørregisterService: AktørregisterService,
        sykepengehistorikkService: SykepengehistorikkService,
        ytelseService: YtelseService
) {
    install(CallId) {
        header("Nav-Call-Id")

        generate { UUID.randomUUID().toString() }
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        if (call.request.path() != "/isready"
                && call.request.path() != "/isalive"
                && call.request.path() != "/metrics") {
            log.info("incoming ${call.request.httpMethod.value} ${call.request.uri}")
        }
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

            arbeidsfordeling(arbeidsfordelingService)

            person(personService)

            arbeidInntektYtelse(arbeidInntektYtelseService)

            arbeidsforhold(arbeidsforholdService, arbeidsgiverService)

            organisasjon(organisasjonService)

            sykepengegrunnlag(sykepengegrunnlagService)

            fnrForAktør(aktørregisterService)

            infotrygdBeregningsgrunnlag(infotrygdBeregningsgrunnlagService)

            sykepengehistorikk(sykepengehistorikkService)

            ytelse(ytelseService)
        }

        nais(collectorRegistry)
    }
}

