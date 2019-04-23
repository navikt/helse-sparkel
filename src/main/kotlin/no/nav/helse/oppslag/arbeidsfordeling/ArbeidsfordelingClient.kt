package no.nav.helse.oppslag.arbeidsfordeling

import arrow.core.Try
import no.nav.helse.domene.person.domain.GeografiskTilknytning
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.ArbeidsfordelingKriterier
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Diskresjonskoder
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Geografi
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeRequest

class ArbeidsfordelingClient(
        private val arbeidsfordelingV1: ArbeidsfordelingV1
) {
    fun getBehandlendeEnhet(
            gjeldendeGeografiskTilknytning: GeografiskTilknytning,
            gjeldendeTema : Tema
    ) =
            Try {
                arbeidsfordelingV1.finnBehandlendeEnhetListe(finnBehandlendeEnhetListeRequest(gjeldendeGeografiskTilknytning, gjeldendeTema)).behandlendeEnhetListe.toList()
            }

    private fun finnBehandlendeEnhetListeRequest(gjeldendeGeografiskTilknytning: GeografiskTilknytning,
                                                 gjeldendeTema : Tema) =
            FinnBehandlendeEnhetListeRequest().apply {
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

}

data class Tema(val value: String)
