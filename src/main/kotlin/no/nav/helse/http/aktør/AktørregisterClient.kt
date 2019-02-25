package no.nav.helse.http.aktør

import com.github.kittinunf.fuel.httpGet
import no.nav.helse.OppslagResult
import no.nav.helse.flatMap
import no.nav.helse.sts.StsRestClient
import org.json.JSONObject
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AktørregisterClient")

class AktørregisterClient(val baseUrl: String, val stsRestClient: StsRestClient) {

    fun gjeldendeIdenter(ident: String): OppslagResult<String, List<Ident>> {
        log.info("lookup gjeldende identer with ident=$ident")

        val bearer = stsRestClient.token()

        val (_, _, result) = "$baseUrl/api/v1/identer?gjeldende=true".httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to "anything",
                        "Nav-Consumer-Id" to "sparkel",
                        "Nav-Personidenter" to ident
                ))
                .responseString()

        val response = JSONObject(result.get())

        val identResponse = response.getJSONObject(ident)

        return if (identResponse.isNull("identer")) {
            OppslagResult.Feil(identResponse.getString("feilmelding"))
        } else {
            val identer = identResponse.getJSONArray("identer")

            OppslagResult.Ok(identer.map {
                it as JSONObject
            }.map {
                Ident(it.getString("ident"), it.getEnum(IdentType::class.java, "identgruppe"))
            })
        }
    }

    private fun gjeldendeIdent(ident: String, type: IdentType): OppslagResult<String, String> {
        return gjeldendeIdenter(ident).flatMap {
            OppslagResult.Ok(it.first {
                it.type == type
            }.ident)
        }
    }

    fun gjeldendeAktørId(ident: String): OppslagResult<String, String> {
        return gjeldendeIdent(ident, IdentType.AktoerId)
    }

    fun gjeldendeNorskIdent(ident: String): OppslagResult<String, String> {
        return gjeldendeIdent(ident, IdentType.NorskIdent)
    }
}

enum class IdentType {
    AktoerId, NorskIdent
}

data class Ident(val ident: String, val type: IdentType)
