package no.nav.helse

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.*
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JsonContentConverterTest {

    @Test
    fun `should serialize map ok`() {
        testJsonResponse(mapOf(
                "id" to 1,
                "title" to "Hello, World!",
                "unicode" to "\u0422"
        )) {
            val expected = """{"id":1,"title":"Hello, World!","unicode":"\u0422"}"""
            assertJsonEquals(JSONObject(expected), JSONObject(it))
        }
    }

    @Test
    fun `should serialize list ok`() {
        testJsonResponse(listOf(mapOf(
                "id" to 1,
                "title" to "Hello, World!",
                "unicode" to "\u0422"
        ))) {
            val expected = """[{"id":1,"title":"Hello, World!","unicode":"\u0422"}]"""
            assertJsonEquals(JSONArray  (expected), JSONArray(it))
        }
    }

    @Test
    fun `should serialize entity ok`() {
        testJsonResponse(TestEntity("World", 42)) {
            val expected = """{"name":"World","age":42}"""
            assertJsonEquals(JSONObject(expected), JSONObject(it))
        }
    }

    private fun testJsonResponse(stubbedResponse: Any, responseAssertion: (String) -> Unit) {
        withTestApplication {
            application.install(ContentNegotiation) {
                register(ContentType.Application.Json, JsonContentConverter())
            }

            application.routing {
                get("/") {
                    call.respond(stubbedResponse)
                }
            }

            handleRequest(HttpMethod.Get, "/") {
                addHeader("Accept", ContentType.Application.Json.toString())
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, response.status())
                Assertions.assertNotNull(response.content)

                val contentTypeText = response.headers[HttpHeaders.ContentType]
                Assertions.assertNotNull(contentTypeText)
                Assertions.assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), ContentType.parse(contentTypeText!!))

                responseAssertion(response.content!!)
            }
        }
    }
}

data class TestEntity(val name: String, val age: Int)
