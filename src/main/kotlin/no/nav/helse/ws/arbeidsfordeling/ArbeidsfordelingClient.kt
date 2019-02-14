package no.nav.helse.ws.arbeidsfordeling

import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.ws.person.GeografiskTilknytning
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.ArbeidsfordelingKriterier
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Diskresjonskoder
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Geografi
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeRequest
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("ArbeidsfordelingClient")

class ArbeidsfordelingClient(
        private val arbeidsfordelingV1: ArbeidsfordelingV1
) {
    fun getBehandlendeEnhet(
            gjeldendeGeografiskTilknytning: GeografiskTilknytning,
            gjeldendeTema : Tema
    ) : OppslagResult {
        val request = FinnBehandlendeEnhetListeRequest().apply {
            arbeidsfordelingKriterier = ArbeidsfordelingKriterier().apply {
                tema = no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Tema().apply {
                    value = gjeldendeTema.value
                }
                if (gjeldendeGeografiskTilknytning.diskresjonskode != null) {
                    diskresjonskode = Diskresjonskoder().apply {
                        value = gjeldendeGeografiskTilknytning.diskresjonskode.forkortelse
                    }
                }

                if (gjeldendeGeografiskTilknytning.geografiskOmraade != null) {
                    geografiskTilknytning = Geografi().apply {
                        value = gjeldendeGeografiskTilknytning.geografiskOmraade.kode
                    }
                }
            }
        }

        return try {
            val enhet = BehandlendeEnhetMapper.tilEnhet(arbeidsfordelingV1.finnBehandlendeEnhetListe(request))
            return if (enhet == null) Failure(listOf("Ingen enheter funnet")) else Success(enhet)
        } catch (cause: Throwable) {
            log.error("Feil ved oppslag på behandlende enhet", cause)
            Failure(listOf(cause.message ?: "Ingen detaljer"))
        }

    }
}

data class Tema(val value: String)