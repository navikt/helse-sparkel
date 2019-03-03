package no.nav.helse.ws.arbeidsfordeling

import no.nav.helse.Feilårsak
import no.nav.helse.flatMap
import no.nav.helse.mapLeft
import no.nav.helse.sequenceU
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.person.PersonService
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.FinnBehandlendeEnhetListeUgyldigInput

class ArbeidsfordelingService(
        private val arbeidsfordelingClient: ArbeidsfordelingClient,
        private val personService: PersonService
) {
    fun getBehandlendeEnhet(
            hovedAktoer: AktørId,
            medAktoerer: List<AktørId> = listOf(),
            tema: Tema
    ) =
            personService.geografiskTilknytning(hovedAktoer).flatMap { geografiskTilknytningHovedAktoer ->
                medAktoerer.map {  medAktoer ->
                    personService.geografiskTilknytning(medAktoer)
                }.sequenceU().flatMap { geografiskTilknytningMedAktoerer ->
                    val gjeldendeGeografiskeTilknytning = geografiskTilknytningMedAktoerer.firstOrNull {
                        it.diskresjonskode?.kode == 6
                    } ?: geografiskTilknytningHovedAktoer

                    arbeidsfordelingClient.getBehandlendeEnhet(gjeldendeGeografiskeTilknytning, tema).mapLeft {
                        when (it) {
                            is IngenEnhetFunnetException -> Feilårsak.IkkeFunnet
                            is FinnBehandlendeEnhetListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                            else -> Feilårsak.UkjentFeil
                        }
                    }
                }
            }
}
