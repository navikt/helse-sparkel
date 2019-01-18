package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis
import redis.embedded.RedisServer
import kotlin.random.Random

class IdentMappingComponentTest {

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        val redisServer = RedisServer(Random.nextInt(1000,9999))

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
            redisServer.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
            redisServer.stop()
        }
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }

    @AfterEach
    fun `reset wiremock and redis`() {
        WireMock.reset()
        val jedis = Jedis("localhost", redisServer.ports().first())
        jedis.flushAll()
    }

    @Test
    fun `no parameter should error`() {
        val jwtStub = JwtStub("test issuer")
        val token = jwtStub.createTokenFor("srvspinne")

        val env = Environment(mapOf(
                "JWT_ISSUER" to "test issuer"
        ))

        withTestApplication({sparkel(env, jwtStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, "api/ident") {
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                Assertions.assertEquals(400, response.status()?.value)
            }
        }
    }

    @Test
    fun `invalid uuid should error`() {
        val jwtStub = JwtStub("test issuer")
        val token = jwtStub.createTokenFor("srvspinne")

        val env = Environment(mapOf(
                "JWT_ISSUER" to "test issuer",
                "REDIS_HOST" to "localhost",
                "REDIS_PORT" to redisServer.ports().first().toString()
        ))

        withTestApplication({sparkel(env, jwtStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, "api/ident?uuid=thisdoesnotexist") {
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                Assertions.assertEquals(404, response.status()?.value)
            }
        }
    }

    @Test
    fun `fnr should return uuid`() {
        val jwtStub = JwtStub("test issuer")
        val token = jwtStub.createTokenFor("srvspinne")

        val env = Environment(mapOf(
                "JWT_ISSUER" to "test issuer",
                "AKTORREGISTER_URL" to server.baseUrl(),
                "SECURITY_TOKEN_SERVICE_REST_URL" to server.baseUrl(),
                "SECURITY_TOKEN_SERVICE_USERNAME" to "username",
                "SECURITY_TOKEN_SERVICE_PASSWORD" to "password",
                "REDIS_HOST" to "localhost",
                "REDIS_PORT" to redisServer.ports().first().toString()
        ))

        WireMock.stubFor(stsRestStub())
        WireMock.stubFor(aktørregisterStub("123456"))

        withTestApplication({sparkel(env, jwtStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, "api/ident?fnr=123456") {
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                Assertions.assertEquals(200, response.status()?.value)
                Assertions.assertTrue(JSONObject(response.content).has("id"))
            }
        }
        WireMock.verify(WireMock.exactly(1), WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/v1/identer")))
    }

    @Test
    fun `fnr should not be fetched on subsequent requests`() {
        val jwtStub = JwtStub("test issuer")
        val token = jwtStub.createTokenFor("srvspinne")

        val env = Environment(mapOf(
                "JWT_ISSUER" to "test issuer",
                "AKTORREGISTER_URL" to server.baseUrl(),
                "SECURITY_TOKEN_SERVICE_REST_URL" to server.baseUrl(),
                "SECURITY_TOKEN_SERVICE_USERNAME" to "username",
                "SECURITY_TOKEN_SERVICE_PASSWORD" to "password",
                "REDIS_HOST" to "localhost",
                "REDIS_PORT" to redisServer.ports().first().toString()
        ))

        WireMock.stubFor(stsRestStub())
        WireMock.stubFor(aktørregisterStub("123456")
                .inScenario("fnr_should_be_cached")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("fnr_has_been_looked_up"))

        withTestApplication({sparkel(env, jwtStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, "api/ident?fnr=123456") {
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                val firstResponse = JSONObject(response.content)

                Assertions.assertEquals(200, response.status()?.value)
                Assertions.assertTrue(firstResponse.has("id"))

                handleRequest(HttpMethod.Get, "api/ident?fnr=123456") {
                    addHeader(HttpHeaders.Authorization, "Bearer ${token}")
                }.apply {
                    val secondResponse = JSONObject(response.content)

                    Assertions.assertEquals(200, response.status()?.value)
                    assertJsonEquals(firstResponse, secondResponse)
                }
            }
        }

        WireMock.verify(WireMock.exactly(1), WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/v1/identer")))
    }

    @Test
    fun `aktørId should return uuid`() {
        val jwtStub = JwtStub("test issuer")
        val token = jwtStub.createTokenFor("srvspinne")

        val env = Environment(mapOf(
                "JWT_ISSUER" to "test issuer",
                "AKTORREGISTER_URL" to server.baseUrl(),
                "SECURITY_TOKEN_SERVICE_REST_URL" to server.baseUrl(),
                "SECURITY_TOKEN_SERVICE_USERNAME" to "username",
                "SECURITY_TOKEN_SERVICE_PASSWORD" to "password",
                "REDIS_HOST" to "localhost",
                "REDIS_PORT" to redisServer.ports().first().toString()
        ))

        WireMock.stubFor(stsRestStub())
        WireMock.stubFor(aktørregisterStub("654321"))

        withTestApplication({sparkel(env, jwtStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, "api/ident?aktorId=654321") {
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                Assertions.assertEquals(200, response.status()?.value)
                Assertions.assertTrue(JSONObject(response.content).has("id"))
            }
        }
        WireMock.verify(WireMock.exactly(1), WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/v1/identer")))
    }

    @Test
    fun `uuid should return aktørId ident`() {
        val jwtStub = JwtStub("test issuer")
        val token = jwtStub.createTokenFor("srvspinne")

        val env = Environment(mapOf(
                "JWT_ISSUER" to "test issuer",
                "AKTORREGISTER_URL" to server.baseUrl(),
                "SECURITY_TOKEN_SERVICE_REST_URL" to server.baseUrl(),
                "SECURITY_TOKEN_SERVICE_USERNAME" to "username",
                "SECURITY_TOKEN_SERVICE_PASSWORD" to "password",
                "REDIS_HOST" to "localhost",
                "REDIS_PORT" to redisServer.ports().first().toString()
        ))

        WireMock.stubFor(stsRestStub())
        WireMock.stubFor(aktørregisterStub("654321", "654321", "123456"))

        withTestApplication({sparkel(env, jwtStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, "api/ident?aktorId=654321") {
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                Assertions.assertEquals(200, response.status()?.value)
                val uuidJson = JSONObject(response.content)

                // map back
                handleRequest(HttpMethod.Get, "/api/ident?uuid=${uuidJson.getString("id")}") {
                    addHeader(HttpHeaders.Authorization, "Bearer ${token}")
                }.apply {
                    val json = JSONArray(response.content)

                    Assertions.assertEquals(200, response.status()?.value)
                    Assertions.assertEquals("654321", json.getJSONObject(0).getString("ident"))
                    Assertions.assertEquals("AktoerId", json.getJSONObject(0).getString("type"))

                    Assertions.assertEquals("123456", json.getJSONObject(1).getString("ident"))
                    Assertions.assertEquals("NorskIdent", json.getJSONObject(1).getString("type"))
                }
            }
        }
        WireMock.verify(WireMock.exactly(1), WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/v1/identer")))
    }

    @Test
    fun `uuid should return fnr ident`() {
        val jwtStub = JwtStub("test issuer")
        val token = jwtStub.createTokenFor("srvspinne")

        val env = Environment(mapOf(
                "JWT_ISSUER" to "test issuer",
                "AKTORREGISTER_URL" to server.baseUrl(),
                "SECURITY_TOKEN_SERVICE_REST_URL" to server.baseUrl(),
                "SECURITY_TOKEN_SERVICE_USERNAME" to "username",
                "SECURITY_TOKEN_SERVICE_PASSWORD" to "password",
                "REDIS_HOST" to "localhost",
                "REDIS_PORT" to redisServer.ports().first().toString()
        ))

        WireMock.stubFor(stsRestStub())
        WireMock.stubFor(aktørregisterStub("123456", "654321", "123456"))

        withTestApplication({sparkel(env, jwtStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, "api/ident?fnr=123456") {
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                Assertions.assertEquals(200, response.status()?.value)
                val uuidJson = JSONObject(response.content)

                // map back
                handleRequest(HttpMethod.Get, "/api/ident?uuid=${uuidJson.getString("id")}") {
                    addHeader(HttpHeaders.Authorization, "Bearer ${token}")
                }.apply {
                    val json = JSONArray(response.content)

                    Assertions.assertEquals(200, response.status()?.value)
                    Assertions.assertEquals("654321", json.getJSONObject(0).getString("ident"))
                    Assertions.assertEquals("AktoerId", json.getJSONObject(0).getString("type"))

                    Assertions.assertEquals("123456", json.getJSONObject(1).getString("ident"))
                    Assertions.assertEquals("NorskIdent", json.getJSONObject(1).getString("type"))
                }
            }
        }
        WireMock.verify(WireMock.exactly(1), WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/v1/identer")))
    }
}
