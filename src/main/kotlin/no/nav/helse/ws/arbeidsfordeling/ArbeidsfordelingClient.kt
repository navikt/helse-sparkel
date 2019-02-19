package no.nav.helse.ws.arbeidsfordeling

import io.ktor.http.HttpStatusCode
import no.nav.helse.Feil
import no.nav.helse.OppslagResult
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
    ) : OppslagResult<Feil, Enhet> {
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
            return if (enhet == null) {
                OppslagResult.Feil(HttpStatusCode.NotFound, Feil.Feilmelding("Ingen enheter funnet"))
            } else {
                OppslagResult.Ok(enhet)
            }
        } catch (cause: Throwable) {
            log.error("Feil ved oppslag på behandlende enhet", cause)
            OppslagResult.Feil(HttpStatusCode.InternalServerError, Feil.Exception(cause.message ?: "Ingen detaljer", cause))
        }

    }
}

data class Tema(val value: String)
