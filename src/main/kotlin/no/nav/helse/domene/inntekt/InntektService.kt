package no.nav.helse.domene.inntekt

import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.oppslag.inntekt.SikkerhetsavvikException
import no.nav.helse.oppslag.inntekt.InntektClient
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkUgyldigInput
import org.slf4j.LoggerFactory
import java.time.YearMonth

class InntektService(private val inntektClient: InntektClient) {

    companion object {
        private val log = LoggerFactory.getLogger(InntektService::class.java)

        private val defaultFilter = "ForeldrepengerA-inntekt"
    }

    fun hentInntekter(aktørId: AktørId, fom: YearMonth, tom: YearMonth, filter: String = defaultFilter) =
            inntektClient.hentInntekter(aktørId, fom, tom, filter).toEither { err ->
                log.error("Error during inntekt lookup", err)

                when (err) {
                    is HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter -> Feilårsak.FeilFraTjeneste
                    is HentInntektListeBolkUgyldigInput -> Feilårsak.FeilFraTjeneste
                    is SikkerhetsavvikException -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.UkjentFeil
                }
            }.map {
                InntektMapper.mapToInntekt(aktørId, fom, tom, it)
            }
}

