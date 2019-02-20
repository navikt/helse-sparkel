package no.nav.helse.ws.arbeidsfordeling

import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Enhetsstatus
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Organisasjonsenhet
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BehandlendeEnhetMapperTest {

    @Test
    fun `skal mappe tom liste til null`() {
        assertNull(BehandlendeEnhetMapper.tilEnhet(FinnBehandlendeEnhetListeResponse()))
    }

    @Test
    fun `skal mappe liste uten aktiv enhet til null`() {
        assertNull(BehandlendeEnhetMapper.tilEnhet(FinnBehandlendeEnhetListeResponse().apply {
            with (behandlendeEnhetListe) {
                add(Organisasjonsenhet().apply {
                    status = Enhetsstatus.NEDLAGT
                })
                add(Organisasjonsenhet().apply {
                    status = Enhetsstatus.UNDER_AVVIKLING
                })
                add(Organisasjonsenhet().apply {
                    status = Enhetsstatus.UNDER_ETABLERING
                })
            }
        }))
    }

    @Test
    fun `skal mappe aktiv organisasjon enhet til enhet`() {
        val enhetId = "4432"
        val enhetNavn = "NAV Arbeid og ytelser Follo"

        val actual = BehandlendeEnhetMapper.tilEnhet(FinnBehandlendeEnhetListeResponse().apply {
            with (behandlendeEnhetListe) {
                add(Organisasjonsenhet().apply {
                    this.status = Enhetsstatus.AKTIV
                    this.enhetId = enhetId
                    this.enhetNavn = enhetNavn
                })
            }
        })

        assertEquals(enhetId, actual?.id)
        assertEquals(enhetNavn, actual?.navn)
    }
}
