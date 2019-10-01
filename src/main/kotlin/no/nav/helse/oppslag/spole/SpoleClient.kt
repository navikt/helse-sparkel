package no.nav.helse.oppslag.spole

import arrow.core.Try
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import no.nav.helse.domene.AktørId
import org.json.JSONObject
import java.time.LocalDate

class SpoleClient(private val baseUrl: String, private val azureClient: AzureClient) {
    fun hentSykepengeperioder(aktørId: AktørId): Try<Sykepengeperioder> {
        return Try {
            val (_, _, result) = "$baseUrl/sykepengeperioder/${aktørId.aktor}".httpGet()
                    .header(mapOf(
                            "Authorization" to "Bearer ${azureClient.fetchToken().accessToken}",
                            "Accept" to "application/json",
                            "Nav-Call-Id" to "anything"
                    ))
                    .responseString()

            val response = JSONObject(result.get())

            Sykepengeperioder(
                    aktørId = response.getString("aktørId"),
                    perioder = response.getJSONArray("perioder").map { it ->
                        it as JSONObject
                    }.map { periodeJson ->
                        Periode(
                                fom = LocalDate.parse(periodeJson.getString("fom")),
                                tom = LocalDate.parse(periodeJson.getString("tom")),
                                grad = periodeJson.getString("grad")
                        )
                    }
            )
        }
    }
}

data class Sykepengeperioder(val aktørId: String, val perioder: List<Periode>)
data class Periode(val fom: LocalDate, val tom: LocalDate, val grad: String)

class AzureClient(private val tenantId: String, private val clientId: String, private val clientSecret: String, private val scope: String) {

    fun fetchToken(): Token {
        val (_, _, result) = "https://login.microsoftonline.com/$tenantId/oauth2/v2.0/token".httpPost(
                listOf(
                        "client_id" to clientId,
                        "client_secret" to clientSecret,
                        "resource" to scope,
                        "grant_type" to "client_credentials"
                )
        ).response()

        val response = JSONObject(result.get())

        return Token(
                tokenType = response.getString("token_type"),
                expiresIn = response.getLong("expires_in"),
                accessToken = response.getString("access_token")
        )
    }

    data class Token(val tokenType: String, val expiresIn: Long, val accessToken: String)
}
