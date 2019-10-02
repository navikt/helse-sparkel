package no.nav.helse.oppslag.spole

import arrow.core.Try
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.domene.AktørId
import org.json.JSONObject
import java.time.LocalDate

class SpoleClient(private val baseUrl: String, private val accesstokenScope: String, private val azureClient: AzureClient) {
    fun hentSykepengeperioder(aktørId: AktørId): Try<Sykepengeperioder> {
        return Try {
            val (_, _, result) = "$baseUrl/sykepengeperioder/${aktørId.aktor}".httpGet()
                    .header(mapOf(
                            "Authorization" to "Bearer ${azureClient.getToken(accesstokenScope).accessToken}",
                            "Accept" to "application/json",
                            "Nav-Call-Id" to "anything"
                    ))
                    .responseString()

            val response = JSONObject(result.get())

            Sykepengeperioder(
                    aktørId = response.getString("aktør_id"),
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

