package no.nav.helse

import com.github.kittinunf.fuel.httpGet
import org.json.JSONObject
import java.time.LocalDateTime

/**
 * henter jwt token fra STS
 */
class StsRestClient(val baseUrl: String, val username: String, val password: String) {
    private var cachedToken: Token? = null

    fun token(): String {
        if (Token.shouldRenew(cachedToken))  {
            val (_, _, result) = "$baseUrl/rest/v1/sts/token?grant_type=client_credentials&scope=openid".httpGet()
                    .authenticate(username, password)
                    .header(mapOf("Accept" to "application/json"))
                    .responseJSON()

            cachedToken = result.get().mapToToken()
        }

        return cachedToken!!.accessToken
    }

    private fun JSONObject.mapToToken(): Token {
        return Token(getString("access_token"),
                getString("token_type"),
                getInt("expires_in"))
    }

    data class Token(val accessToken: String, val type: String, val expiresIn: Int) {
        // expire 10 seconds before actual expiry. for great margins.
        val expirationTime: LocalDateTime = LocalDateTime.now().plusSeconds(expiresIn - 10L)

        companion object {
            fun shouldRenew(token: Token?): Boolean {
                if (token == null) {
                    return true
                }

                return Token.isExpired(token)
            }

            fun isExpired(token: Token): Boolean {
                return token.expirationTime.isBefore(LocalDateTime.now())
            }
        }
    }
}
