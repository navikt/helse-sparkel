package no.nav.helse.ws.sykepenger

import com.github.kittinunf.fuel.httpGet
import no.nav.helse.Either
import no.nav.helse.sts.StsRestClient
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Sykmeldingsperiode
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HentSykepengeListeRestClient(val baseUrl: String, val stsRestClient: StsRestClient) {

    fun hentSykepengeListe(fnr: String): Either<Exception, List<SykepengerPeriode>> {
        val bearer = stsRestClient.token()

        val (_, _, result) = "$baseUrl/hentSykepengerListe?fnr=$fnr".httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to "anything",
                        "Nav-Consumer-Id" to "sparkel"
                ))
                .responseString()
        return try {
            Either.Right(JSONObject(result.get()).toListOfSykepengerPeriode())
        } catch(e: Exception) {
            Either.Left(e)
        }
    }
}

data class SykepengerPeriode(val fom: LocalDate,
                             val tom: LocalDate,
                             val dagsats: BigDecimal,
                             val grad: Float)

private fun JSONObject.getLocalDate(field: String) = LocalDate.parse(getString(field), DateTimeFormatter.ISO_LOCAL_DATE)

fun JSONObject.toListOfSykepengerPeriode() = this.getJSONArray("sykmeldingsperioder")
        .flatMap { sykemeldingsperiode ->
            (sykemeldingsperiode as JSONObject).getJSONArray("utbetalingList").map { utbetaling: Any ->
                when (utbetaling) {
                    is JSONObject -> SykepengerPeriode(
                            fom = utbetaling.getLocalDate("fom"),
                            tom = utbetaling.getLocalDate("tom"),
                            dagsats = utbetaling.getBigDecimal("dagsats"),
                            grad = utbetaling.getFloat("utbetalingsGrad")
                    )
                    else -> null
                }
            }
                    .filter { it != null }
                    .requireNoNulls()
        }
