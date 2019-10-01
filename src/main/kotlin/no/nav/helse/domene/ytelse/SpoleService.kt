package no.nav.helse.domene.ytelse

import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.oppslag.spole.SpoleClient
import org.slf4j.LoggerFactory

class SpoleService(private val spoleClient: SpoleClient) {

    private val log = LoggerFactory.getLogger(SpoleService::class.java)

    fun hentSykepengeperioder(aktørId: AktørId) =
            spoleClient.hentSykepengeperioder(aktørId).toEither { err ->
                log.error("received error during lookup", err)
                Feilårsak.FeilFraTjeneste
            }.map {
                it.perioder
            }
}
