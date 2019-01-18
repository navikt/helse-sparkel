package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.prometheus.client.CollectorRegistry
import no.nav.helse.ws.samlAssertionResponse
import no.nav.helse.ws.stsStub
import redis.clients.jedis.Jedis
import redis.embedded.RedisServer
import kotlin.random.Random

fun bootstrapComponentTest() : ComponentTestBootstrap {
    val wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    wireMockServer.start()
    WireMock.configureFor(wireMockServer.port())

    WireMock.stubFor(stsRestStub())
    WireMock.stubFor(stsStub("stsUsername", "stsPassword")
            .willReturn(samlAssertionResponse("testusername", "theIssuer", "CN=B27 Issuing CA Intern, DC=preprod, DC=local",
                    "digestValue", "signatureValue", "certificateValue")))

    val redisServer = RedisServer(Random.nextInt(1000,9999))
    val jwkStub = JwtStub("test issuer")
    val env = Environment(mapOf(
            "JWT_ISSUER" to "test issuer",
            "AKTORREGISTER_URL" to wireMockServer.baseUrl(),
            "SECURITY_TOKEN_SERVICE_REST_URL" to wireMockServer.baseUrl(),
            "SECURITY_TOKEN_SERVICE_URL" to wireMockServer.baseUrl().plus("/sts"),
            "SECURITY_TOKEN_SERVICE_USERNAME" to "stsUsername",
            "SECURITY_TOKEN_SERVICE_PASSWORD" to "stsPassword",
            "REDIS_HOST" to "localhost",
            "REDIS_PORT" to redisServer.ports().first().toString(),
            "AAREG_ENDPOINTURL" to wireMockServer.baseUrl().plus("/aareg"),
            "ALLOW_INSECURE_SOAP_REQUESTS" to "true"
    ))

    return ComponentTestBootstrap(
            env = env,
            jwkStub = jwkStub,
            redisServer = redisServer,
            wireMockServer = wireMockServer
    )
}

data class ComponentTestBootstrap(
        val env: Environment,
        val jwkStub: JwtStub,
        val redisServer: RedisServer,
        val wireMockServer: WireMockServer
) {

    fun start() {
        redisServer.start()
    }

    fun stop() {
        wireMockServer.stop()
        redisServer.stop()
    }

    fun reset() {
        WireMock.reset()
        val jedis = Jedis("localhost", redisServer.ports().first())
        jedis.flushAll()
        CollectorRegistry.defaultRegistry.clear()
    }
}
