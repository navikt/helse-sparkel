package no.nav.helse

import com.github.kittinunf.fuel.httpGet
import org.json.JSONObject
import java.time.LocalDateTime

/**
 * henter jwt token fra STS
 */
class StsRestClient(val baseUrl: String, val username: String, val password: String) {
    private var cachedToken: JSONObject = JSONObject()
    private var expiryDateTime:LocalDateTime = LocalDateTime.now().minusDays(14L)

    fun token(): String {
        if (expiryDateTime.isBefore(LocalDateTime.now()))  {
            val (_, _, result) = "$baseUrl/rest/v1/sts/token?grant_type=client_credentials&scope=openid".httpGet()
                    .authenticate(username, password)
                    .header(mapOf("Accept" to "application/json"))
                    .response()

            cachedToken = JSONObject(String(result.component1()!!))

            // expire 10 seconds before actual expiry. for great margins.
            expiryDateTime = LocalDateTime.now().plusSeconds(cachedToken.getLong("expires_in") - 10L)
        }

        return cachedToken.getString("access_token")
    }
}
