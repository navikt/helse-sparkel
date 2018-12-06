package no.nav.helse.ws.inntekt

import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.hentinntektliste.ArbeidsInntektInformasjon
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest
import org.slf4j.LoggerFactory
import java.time.YearMonth
import javax.xml.datatype.DatatypeFactory


class InntektClient(private val inntektV3: InntektV3) {
    private val log = LoggerFactory.getLogger(InntektClient::class.java)

    fun hentInntektListe(fødelsnummer: String): OppslagResult {
        val request = HentInntektListeBolkRequest().apply {
            identListe.add(PersonIdent().apply {
                personIdent = fødelsnummer
            })
            formaal = Formaal().apply {
                value = "Sykepenger"
            }
            ainntektsfilter = Ainntektsfilter().apply {
                value = "SykepengerA-Inntekt"
            }
            uttrekksperiode = Uttrekksperiode().apply {
                maanedFom = DatatypeFactory.newInstance().newXMLGregorianCalendar(2017, 12, 1, 0, 0, 0, 0, 0)
                maanedTom = DatatypeFactory.newInstance().newXMLGregorianCalendar(2018, 1, 1, 0, 0, 0, 0, 0)
            }
        }

        try {
            val response = inntektV3.hentInntektListeBolk(request)

            return Success(response)
        } catch (ex: Exception) {
            log.error("Error during inntekt lookup", ex)
            return Failure(listOf(ex.message ?: "unknown error"))
        }
    }

    data class HentInntektListeResponse(
            val inntekter: List<ArbeidsInntektMaaned>
    )
    data class ArbeidsInntektMaaned(
            val årMåned: YearMonth,
            val avvik: List<Avvik>,
            val arbeidsInntektInformasjon: ArbeidsInntektInformasjon
    )
    data class Avvik(
            val ident: Aktoer,
            val opplysningspliktig: Aktoer,
            val virksomhet: Aktoer,
            val avvikPeriode: YearMonth,
            val tekst: String
    )
}

