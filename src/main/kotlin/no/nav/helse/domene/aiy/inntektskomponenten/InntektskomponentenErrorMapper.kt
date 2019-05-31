package no.nav.helse.domene.aiy.inntektskomponenten

import no.nav.helse.Feilårsak
import no.nav.helse.oppslag.inntekt.SikkerhetsavvikException
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkUgyldigInput
import org.slf4j.LoggerFactory

object InntektskomponentenErrorMapper {

    private val log = LoggerFactory.getLogger(InntektskomponentenErrorMapper::class.java)

    fun mapToError(err: Throwable) =
            when (err) {
                is HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter -> Feilårsak.FeilFraTjeneste
                is HentInntektListeBolkUgyldigInput -> Feilårsak.FeilFraTjeneste
                is SikkerhetsavvikException -> Feilårsak.FeilFraTjeneste
                else -> Feilårsak.UkjentFeil
            }.also {
                log.error("received error during lookup, mapping to $it", err)
            }

}
