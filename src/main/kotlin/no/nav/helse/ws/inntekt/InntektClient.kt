package no.nav.helse.ws.inntekt

import no.nav.helse.OppslagResult
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Ainntektsfilter
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.AktoerId
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Formaal
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Uttrekksperiode
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse
import org.slf4j.LoggerFactory
import java.time.YearMonth


class InntektClient(private val inntektV3: InntektV3) {
    private val log = LoggerFactory.getLogger(InntektClient::class.java)

    fun hentInntektListe(aktørId: AktørId, fom: YearMonth, tom: YearMonth): OppslagResult<Exception, HentInntektListeBolkResponse> {
        val request = HentInntektListeBolkRequest().apply {
            identListe.add(AktoerId().apply {
                aktoerId = aktørId.aktor
            })
            formaal = Formaal().apply {
                value = "Foreldrepenger"
            }
            ainntektsfilter = Ainntektsfilter().apply {
                value = "ForeldrepengerA-Inntekt"
            }
            uttrekksperiode = Uttrekksperiode().apply {
                maanedFom = fom.toXmlGregorianCalendar()
                maanedTom = tom.toXmlGregorianCalendar()
            }
        }

        return try {
            OppslagResult.Ok(inntektV3.hentInntektListeBolk(request))
        } catch (ex: Exception) {
            log.error("Error during inntekt lookup", ex)
            OppslagResult.Feil(ex)
        }
    }
}

