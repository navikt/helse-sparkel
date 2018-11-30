package no.nav.helse.ws.inntekt

import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.tjeneste.virksomhet.inntekt.v3.InntektV3
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.hentinntektliste.ArbeidsInntektInformasjon
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Aktoer
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.PersonIdent
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeRequest
import org.slf4j.LoggerFactory
import java.time.YearMonth


class InntektClient(private val inntektV3Factory: () -> InntektV3) {
    private val log = LoggerFactory.getLogger(InntektClient::class.java)
    private val inntektV3: InntektV3 get() = inntektV3Factory()

    fun hentInntektListe(fødelsnummer: String): OppslagResult {
        val request = HentInntektListeRequest().apply {
            ident = PersonIdent().apply {
                personIdent = fødelsnummer
            }
        }

        try {
            val response = inntektV3.hentInntektListe(request)

            return Success(HentInntektListeResponse(
                    response.arbeidsInntektIdent.arbeidsInntektMaaned.map { InntektClient.ArbeidsInntektMaaned(
                            it.aarMaaned.let { YearMonth.of(it.year, it.month) },
                            it.avvikListe.map { Avvik(it.ident,
                                    it.opplysningspliktig,
                                    it.virksomhet,
                                    it.avvikPeriode.let { YearMonth.of(it.year, it.month) },
                                    it.tekst
                            ) },
                            it.arbeidsInntektInformasjon
                    ) }
            ))
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

