package no.nav.helse

import io.ktor.application.call
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class HttpRequestMetricTest {

    companion object {
        private val log = LoggerFactory.getLogger(HttpRequestMetricTest::class.java)
    }

    @Test
    fun `counter should increase on faulty route`() {
        assertThatRequestCounterIncrease(HttpMethod.Get, "/api/faulty", HttpStatusCode.InternalServerError)
    }

    @Test
    fun `counter should increase on non-existing route`() {
        assertThatRequestCounterIncrease(HttpMethod.Get, "/api/this-does-not-exist", HttpStatusCode.NotFound)
    }

    @Test
    fun `counter should increase on ok route`() {
        assertThatRequestCounterIncrease(HttpMethod.Get, "/api/ok", HttpStatusCode.OK)
    }

    @Test
    fun `counter should increase on route which does not set status code explicitly`() {
        assertThatRequestCounterIncrease(HttpMethod.Get, "/api/ok-without-explicit-status-code", HttpStatusCode.OK)
    }

    @Test
    fun `counter should increase on route which responds with error`() {
      assertThatRequestCounterIncrease(HttpMethod.Get, "/api/503", HttpStatusCode.ServiceUnavailable)
    }

    private fun assertThatRequestCounterIncrease(method: HttpMethod, path: String, expectedStatusCode: HttpStatusCode) {
        testServer {
            val requestCounterBefore = getCounterValue("http_requests_total", listOf(method.value, "${expectedStatusCode.value}"))
            val requestHistogramBefore = getCounterValue("http_request_duration_seconds_count")

            handleRequest(method, path, {
                setRequestProperty(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }) { responseStatus ->
                assertEquals(expectedStatusCode, responseStatus)

                val requestHistogramAfter = getCounterValue("http_request_duration_seconds_count")
                val requestCounterAfter = getCounterValue("http_requests_total", listOf(method.value, "${expectedStatusCode.value}"))

                assertEquals(1, requestCounterAfter - requestCounterBefore)
                assertEquals(1, requestHistogramAfter - requestHistogramBefore)
            }
        }
    }

    private fun getCounterValue(name: String, labelValues: List<String> = emptyList()) =
            (CollectorRegistry.defaultRegistry
                    .findMetricSample(name, labelValues)
                    ?.value ?: 0.0).toInt()

    private fun CollectorRegistry.findMetricSample(name: String, labelValues: List<String>) =
            findSamples(name).firstOrNull { sample ->
                sample.labelValues.size == labelValues.size && sample.labelValues.containsAll(labelValues)
            }

    private fun CollectorRegistry.findSamples(name: String) =
            filteredMetricFamilySamples(setOf(name))
                    .toList()
                    .flatMap { metricFamily ->
                        metricFamily.samples
                    }

    private fun testServer(test: ApplicationEngine.() -> Unit) = embeddedServer(Netty, Random.nextInt(1000, 9999)) {
        mockedSparkel(
                jwkProvider = JwtStub("test issuer").stubbedJwkProvider()
        )

        routing {
            get("api/faulty") {
                throw RuntimeException("shit happend")
            }
            get("api/ok") {
                call.respond(HttpStatusCode.OK, TextContent("Hello, World!", ContentType.Text.Plain
                        .withCharset(Charsets.UTF_8)))
            }
            get("api/ok-without-explicit-status-code") {
                call.respond(TextContent("Hello, World!", ContentType.Text.Plain
                        .withCharset(Charsets.UTF_8)))
            }
            get("api/503") {
                call.respond(HttpStatusCode.ServiceUnavailable, TextContent("Service Unavailable", ContentType.Text.Plain
                        .withCharset(Charsets.UTF_8)))
            }
        }
    }.apply {
        val stopper = GlobalScope.launch {
            delay(10000)
            log.info("stopping server after timeout")
            stop(0, 0, TimeUnit.SECONDS)
        }

        start(wait = false)
        try {
            test()
        } finally {
            stopper.cancel()
            stop(0, 0, TimeUnit.SECONDS)
        }
    }

    private fun ApplicationEngine.handleRequest(method: HttpMethod,
                                                path: String,
                                                builder: HttpURLConnection.() -> Unit,
                                                test: (HttpStatusCode) -> Unit) {
        val url = environment.connectors[0].let { connector ->
            URL("${connector.type.name.toLowerCase()}://${connector.host}:${connector.port}$path")
        }
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = method.value

        con.builder()

        con.connectTimeout = 1000
        con.readTimeout = 1000

        test(HttpStatusCode.fromValue(con.responseCode))
    }
}
