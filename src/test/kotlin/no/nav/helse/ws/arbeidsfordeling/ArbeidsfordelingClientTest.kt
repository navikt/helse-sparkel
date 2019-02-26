package no.nav.helse.ws.arbeidsfordeling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.OppslagResult
import no.nav.helse.ws.person.Diskresjonskode
import no.nav.helse.ws.person.GeografiskOmraade
import no.nav.helse.ws.person.GeografiskTilknytning
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Enhetsstatus
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Enhetstyper
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Organisasjonsenhet
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ArbeidsfordelingClientTest {

    @Test
    fun `skal søke etter enhet basert på tema når diskresjonskode og geografisk område er null`() {
        val arbeidsfordelingV1 = mockk<ArbeidsfordelingV1>()

        every {
            arbeidsfordelingV1.finnBehandlendeEnhetListe(match {
                with(it.arbeidsfordelingKriterier) {
                   tema.value == "SYK" && diskresjonskode == null && geografiskTilknytning == null
                }
            })
        } returns FinnBehandlendeEnhetListeResponse().apply {
            with (behandlendeEnhetListe) {
                add(Organisasjonsenhet().apply {
                    enhetId = "4432"
                    enhetNavn = "NAV Arbeid og ytelser Follo"
                    status = Enhetsstatus.AKTIV
                    type = Enhetstyper().apply {
                        value = "YTA"
                    }
                })
            }
        }

        val arbeidsfordelingClient = ArbeidsfordelingClient(arbeidsfordelingV1)

        val geografiskTilknytning = GeografiskTilknytning(null, null)
        val tema = Tema("SYK")
        val actual = arbeidsfordelingClient.getBehandlendeEnhet(geografiskTilknytning, tema)

        verify(exactly = 1) {
            arbeidsfordelingV1.finnBehandlendeEnhetListe(any())
        }

        val expected = Enhet("4432", "NAV Arbeid og ytelser Follo")

        when (actual) {
            is OppslagResult.Ok -> assertEquals(expected, actual.data)
            else -> fail { "Expected OppslagResult.Ok to be returned" }
        }
    }

    @Test
    fun `skal søke etter enhet basert på tema og diskresjonskode når geografisk område er null`() {
        val arbeidsfordelingV1 = mockk<ArbeidsfordelingV1>()

        every {
            arbeidsfordelingV1.finnBehandlendeEnhetListe(match {
                with(it.arbeidsfordelingKriterier) {
                    tema.value == "SYK" && diskresjonskode.value == "SPSF" && geografiskTilknytning == null
                }
            })
        } returns FinnBehandlendeEnhetListeResponse().apply {
            with (behandlendeEnhetListe) {
                add(Organisasjonsenhet().apply {
                    enhetId = "4432"
                    enhetNavn = "NAV Arbeid og ytelser Follo"
                    status = Enhetsstatus.AKTIV
                    type = Enhetstyper().apply {
                        value = "YTA"
                    }
                })
            }
        }

        val arbeidsfordelingClient = ArbeidsfordelingClient(arbeidsfordelingV1)

        val geografiskTilknytning = GeografiskTilknytning(Diskresjonskode("SPSF"), null)
        val tema = Tema("SYK")
        val actual = arbeidsfordelingClient.getBehandlendeEnhet(geografiskTilknytning, tema)

        verify(exactly = 1) {
            arbeidsfordelingV1.finnBehandlendeEnhetListe(any())
        }

        val expected = Enhet("4432", "NAV Arbeid og ytelser Follo")

        when (actual) {
            is OppslagResult.Ok -> assertEquals(expected, actual.data)
            else -> fail { "Expected OppslagResult.Ok to be returned" }
        }
    }

    @Test
    fun `skal søke etter enhet basert på tema, diskresjonskode og geografisk område`() {
        val arbeidsfordelingV1 = mockk<ArbeidsfordelingV1>()

        every {
            arbeidsfordelingV1.finnBehandlendeEnhetListe(match {
                with(it.arbeidsfordelingKriterier) {
                    tema.value == "SYK" && diskresjonskode.value == "SPSF" && geografiskTilknytning.value == "030103"
                }
            })
        } returns FinnBehandlendeEnhetListeResponse().apply {
            with (behandlendeEnhetListe) {
                add(Organisasjonsenhet().apply {
                    enhetId = "4432"
                    enhetNavn = "NAV Arbeid og ytelser Follo"
                    status = Enhetsstatus.AKTIV
                    type = Enhetstyper().apply {
                        value = "YTA"
                    }
                })
            }
        }

        val arbeidsfordelingClient = ArbeidsfordelingClient(arbeidsfordelingV1)

        val geografiskTilknytning = GeografiskTilknytning(Diskresjonskode("SPSF"), GeografiskOmraade("BYDEL", "030103"))
        val tema = Tema("SYK")
        val actual = arbeidsfordelingClient.getBehandlendeEnhet(geografiskTilknytning, tema)

        verify(exactly = 1) {
            arbeidsfordelingV1.finnBehandlendeEnhetListe(any())
        }

        val expected = Enhet("4432", "NAV Arbeid og ytelser Follo")

        when (actual) {
            is OppslagResult.Ok -> assertEquals(expected, actual.data)
            else -> fail { "Expected OppslagResult.Ok to be returned" }
        }
    }

    @Test
    fun `skal gi feil når ingen enhet finnes`() {
        val arbeidsfordelingV1 = mockk<ArbeidsfordelingV1>()

        every {
            arbeidsfordelingV1.finnBehandlendeEnhetListe(any())
        } returns FinnBehandlendeEnhetListeResponse()

        val arbeidsfordelingClient = ArbeidsfordelingClient(arbeidsfordelingV1)

        val geografiskTilknytning = GeografiskTilknytning(null, null)
        val tema = Tema("SYK")
        val actual = arbeidsfordelingClient.getBehandlendeEnhet(geografiskTilknytning, tema)

        verify(exactly = 1) {
            arbeidsfordelingV1.finnBehandlendeEnhetListe(any())
        }

        when (actual) {
            is OppslagResult.Feil -> {
                assertTrue(actual.feil is IngenEnhetFunnetException)
            }
            else -> fail { "Expected OppslagResult.Feil to be returned" }
        }
    }

    @Test
    fun `skal gi feil når oppslag gir feil`() {
        val arbeidsfordelingV1 = mockk<ArbeidsfordelingV1>()

        every {
            arbeidsfordelingV1.finnBehandlendeEnhetListe(any())
        } throws(Exception("SOAP fault"))

        val arbeidsfordelingClient = ArbeidsfordelingClient(arbeidsfordelingV1)

        val geografiskTilknytning = GeografiskTilknytning(null, null)
        val tema = Tema("SYK")
        val actual = arbeidsfordelingClient.getBehandlendeEnhet(geografiskTilknytning, tema)

        verify(exactly = 1) {
            arbeidsfordelingV1.finnBehandlendeEnhetListe(any())
        }

        when (actual) {
            is OppslagResult.Feil -> assertEquals("SOAP fault", actual.feil.message)
            else -> fail { "Expected OppslagResult.Feil to be returned" }
        }
    }
}