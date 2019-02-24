package no.nav.helse.ws.arbeidsfordeling

import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Enhetsstatus
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeResponse

object BehandlendeEnhetMapper {
    fun tilEnhet(response: FinnBehandlendeEnhetListeResponse) : Enhet? {
        if (response.behandlendeEnhetListe == null || response.behandlendeEnhetListe.isEmpty()) {
            return null
        }
        val enhet = response.behandlendeEnhetListe.firstOrNull { it.status == Enhetsstatus.AKTIV }
        return if (enhet == null) null else Enhet(id = enhet.enhetId, navn = enhet.enhetNavn)
    }
}

data class Enhet(val id: String, val navn: String)
