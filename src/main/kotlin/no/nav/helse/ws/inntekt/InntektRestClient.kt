package no.nav.helse.ws.inntekt

import com.github.kittinunf.fuel.httpPost
import no.nav.helse.StsRestClient
import org.json.JSONObject
import java.time.YearMonth

class InntektRestClient(val baseUrl: String, val stsRestClient: StsRestClient) {
    enum class IdentType {
        NATURLIG_IDENT, AKTOER_ID
    }
    fun hentInntektListe(ident: String, månedFom: YearMonth, type: IdentType): JSONObject {
        val bearer = stsRestClient.token()

        val requestBody = JSONObject(mapOf(
                "ident" to mapOf(
                        "identifikator" to ident,
                        "aktoerType" to type
                ),
                "maanedFom" to månedFom
        ))
        val (_, _, result) = "$baseUrl/api/v1/hentinntektliste".httpPost()
                .jsonBody(requestBody.toString())
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to "anything",
                        "Nav-Consumer-Id" to "sparkel"
                ))
                .responseString()

        return JSONObject(result.get())
    }
}
