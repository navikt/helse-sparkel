package no.nav.helse.http.aktør

import com.github.kittinunf.fuel.httpGet
import no.nav.helse.Either
import no.nav.helse.flatMap
import no.nav.helse.sts.StsRestClient
import org.json.JSONObject
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AktørregisterClient")

class AktørregisterClient(val baseUrl: String, val stsRestClient: StsRestClient) {

    fun gjeldendeIdenter(ident: String): Either<String, List<Ident>> {
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
            Either.Left(identResponse.getString("feilmelding"))
        } else {
            val identer = identResponse.getJSONArray("identer")

            Either.Right(identer.map {
                it as JSONObject
            }.map {
                Ident(it.getString("ident"), it.getEnum(IdentType::class.java, "identgruppe"))
            })
        }
    }

    private fun gjeldendeIdent(ident: String, type: IdentType): Either<String, String> {
        return gjeldendeIdenter(ident).flatMap {
            Either.Right(it.first {
                it.type == type
            }.ident)
        }
    }

    fun gjeldendeAktørId(ident: String): Either<String, String> {
        return gjeldendeIdent(ident, IdentType.AktoerId)
    }

    fun gjeldendeNorskIdent(ident: String): Either<String, String> {
        return gjeldendeIdent(ident, IdentType.NorskIdent)
    }
}

enum class IdentType {
    AktoerId, NorskIdent
}

data class Ident(val ident: String, val type: IdentType)
