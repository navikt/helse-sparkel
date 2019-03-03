package no.nav.helse.ws.arbeidsfordeling

import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Enhetsstatus
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Organisasjonsenhet

object BehandlendeEnhetMapper {
    fun tilEnhet(behandlendeEnhetListe: List<Organisasjonsenhet>) =
            behandlendeEnhetListe.firstOrNull {
                it.status == Enhetsstatus.AKTIV
            }?.let {enhet ->
                Enhet(enhet.enhetId, enhet.enhetNavn)
            }
}

data class Enhet(val id: String, val navn: String)
