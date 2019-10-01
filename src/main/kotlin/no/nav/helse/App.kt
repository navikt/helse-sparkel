package no.nav.helse

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallId
import io.ktor.features.ContentNegotiation
import io.ktor.features.callId
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.request.userAgent
import io.ktor.response.ApplicationSendPipeline
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.domene.aiy.ArbeidInntektYtelseService
import no.nav.helse.domene.aiy.ArbeidsforholdService
import no.nav.helse.domene.aiy.SykepengegrunnlagService
import no.nav.helse.domene.aiy.aareg.ArbeidsgiverService
import no.nav.helse.domene.aiy.aareg.ArbeidstakerService
import no.nav.helse.domene.aiy.inntektskomponenten.FrilansArbeidsforholdService
import no.nav.helse.domene.aiy.inntektskomponenten.UtbetalingOgTrekkService
import no.nav.helse.domene.aiy.organisasjon.OrganisasjonService
import no.nav.helse.domene.aiy.organisasjon.organisasjon
import no.nav.helse.domene.aiy.web.arbeidInntektYtelse
import no.nav.helse.domene.aiy.web.arbeidsforhold
import no.nav.helse.domene.aiy.web.sykepengegrunnlag
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.aktør.fnrForAktør
import no.nav.helse.domene.arbeidsfordeling.ArbeidsfordelingService
import no.nav.helse.domene.arbeidsfordeling.arbeidsfordeling
import no.nav.helse.domene.person.PersonService
import no.nav.helse.domene.person.person
import no.nav.helse.domene.ytelse.SpoleService
import no.nav.helse.domene.ytelse.YtelseService
import no.nav.helse.domene.ytelse.arena.ArenaService
import no.nav.helse.domene.ytelse.infotrygd.InfotrygdProbe
import no.nav.helse.domene.ytelse.infotrygd.InfotrygdService
import no.nav.helse.domene.ytelse.sykepengehistorikk.SykepengehistorikkService
import no.nav.helse.domene.ytelse.sykepengehistorikk.sykepengehistorikk
import no.nav.helse.domene.ytelse.ytelse
import no.nav.helse.nais.nais
import no.nav.helse.oppslag.WsClients
import no.nav.helse.oppslag.spole.AzureClient
import no.nav.helse.oppslag.spole.SpoleClient
import no.nav.helse.oppslag.sts.stsClient
import no.nav.helse.probe.DatakvalitetProbe
import no.nav.helse.probe.InfluxMetricReporter
import no.nav.helse.probe.SensuClient
import no.nav.helse.sts.StsRestClient
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

private val collectorRegistry = CollectorRegistry.defaultRegistry
private val authorizedUsers = listOf("srvspa", "srvpleiepengesokna", "srvpleiepenger-opp", "srvspenn", "srvspeil")

private val httpRequestCounter = Counter.build("http_requests_total", "Counts the http requests")
        .labelNames("method", "code")
        .register()

private val httpRequestDuration = Histogram.build("http_request_duration_seconds", "Distribution of http request duration")
        .register()

private val httpTraceLog = LoggerFactory.getLogger("HttpTraceLog")

val callIdGenerator = ThreadLocal.withInitial {
    UUID.randomUUID().toString()
}

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

        val wsClients = WsClients(
                stsClientWs = stsClientWs,
                stsClientRest = stsClientRest,
                callIdGenerator = callIdGenerator::get
        )

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

        val arbeidstakerService = ArbeidstakerService(
                arbeidsforholdClient = arbeidsforholdClient,
                datakvalitetProbe = datakvalitetProbe
        )

        val arbeidsforholdService = ArbeidsforholdService(
                arbeidstakerService = arbeidstakerService,
                frilansArbeidsforholdService = FrilansArbeidsforholdService(
                    inntektClient = inntektClient,
                    datakvalitetProbe = datakvalitetProbe
                )
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

        val infotrygdService = InfotrygdService(
                infotrygdBeregningsgrunnlagClient = wsClients.infotrygdBeregningsgrunnlag(env.finnInfotrygdGrunnlagListeEndpointUrl),
                infotrygdSakClient = wsClients.infotrygdSak(env.infotrygdSakEndpoint),
                probe = InfotrygdProbe(InfluxMetricReporter(sensuClient, "sparkel-events", mapOf(
                        "application" to (System.getenv("NAIS_APP_NAME") ?: "sparkel"),
                        "cluster" to (System.getenv("NAIS_CLUSTER_NAME") ?: "dev-fss"),
                        "namespace" to (System.getenv("NAIS_NAMESPACE") ?: "default")
                )))
        )
        val sykepengehistorikkService = SykepengehistorikkService(
                infotrygdService = infotrygdService,
                aktørregisterService = aktørregisterService,
                spoleService = SpoleService(
                        spoleClient = SpoleClient(env.spoleUrl, AzureClient(
                                tenantId = env.azureTenantId,
                                clientId = env.azureClientId,
                                clientSecret = env.azureClientSecret,
                                scope = env.spoleScope
                        ))
                )
        )

        val ytelseService = YtelseService(
                aktørregisterService = aktørregisterService,
                infotrygdService = infotrygdService,
                arenaService = ArenaService(
                        meldekortUtbetalingsgrunnlagClient = wsClients.meldekortUtbetalingsgrunnlag(env.meldekortEndpointUrl)
                )
        )

        sparkel(
                env.jwtIssuer,
                jwkProvider,
                arbeidsfordelingService,
                arbeidstakerService,
                arbeidsgiverService,
                arbeidsforholdMedInntektService,
                organisasjonService,
                personService,
                sykepengegrunnlagService,
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
        arbeidstakerService: ArbeidstakerService,
        arbeidsgiverService: ArbeidsgiverService,
        arbeidInntektYtelseService: ArbeidInntektYtelseService,
        organisasjonService: OrganisasjonService,
        personService: PersonService,
        sykepengegrunnlagService: SykepengegrunnlagService,
        aktørregisterService: AktørregisterService,
        sykepengehistorikkService: SykepengehistorikkService,
        ytelseService: YtelseService
) {
    install(CallId) {
        header("Nav-Call-Id")

        generate {
            callIdGenerator.get()
        }

        reply { _, callId ->
            callIdGenerator.set(callId)
        }
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        withCommonMDC(call) {
            val timer = httpRequestDuration.startTimer()

            httpTraceLog.info("incoming ${call.request.httpMethod.value} ${call.request.uri}")

            try {
                proceed()
            } catch (err: Throwable) {
                httpTraceLog.info("exception thrown during processing: ${err.message}", err)
                throw err
            } finally {
                timer.observeDuration()
            }
        }
    }

    sendPipeline.intercept(ApplicationSendPipeline.Before) {
        withCommonMDC(call) {
            proceed()
        }
    }

    sendPipeline.intercept(ApplicationSendPipeline.After) { message ->
        val status = call.response.status() ?: (when (message) {
            is OutgoingContent -> message.status
            is HttpStatusCode -> message
            else -> null
        } ?: HttpStatusCode.OK).also { status ->
            call.response.status(status)
        }

        withCommonMDC(call) {
            httpTraceLog.info("responding with ${status.value}")
            httpRequestCounter.labels(call.request.httpMethod.value, "${status.value}").inc()
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

            arbeidsforhold(arbeidstakerService, arbeidsgiverService)

            organisasjon(organisasjonService)

            sykepengegrunnlag(sykepengegrunnlagService)

            fnrForAktør(aktørregisterService)

            sykepengehistorikk(sykepengehistorikkService)

            ytelse(ytelseService)
        }

        nais(collectorRegistry)
    }
}

suspend fun withCommonMDC(call: ApplicationCall, block: suspend () -> Unit) {
    withMDC {
        MDC.put("call_id", call.callId)
        MDC.put("scheme", call.request.local.scheme)
        MDC.put("host", call.request.local.host)
        MDC.put("remote_host", call.request.local.remoteHost)
        MDC.put("port", "${call.request.local.port}")
        MDC.put("request_method", call.request.httpMethod.value)
        MDC.put("request_uri", call.request.uri)
        call.request.userAgent()?.let { agent ->
            MDC.put("user_agent", agent)
        }
        call.response.status()?.let { status ->
            MDC.put("http_code", "${status.value}")
        }

        block()
    }
}

suspend fun withMDC(block: suspend () -> Unit) {
    val oldState = MDC.getCopyOfContextMap() ?: emptyMap()
    try {
        block()
    } finally {
        MDC.setContextMap(oldState)
    }
}
