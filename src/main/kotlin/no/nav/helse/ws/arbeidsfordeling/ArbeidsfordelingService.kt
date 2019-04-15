package no.nav.helse.ws.arbeidsfordeling

import arrow.core.flatMap
import arrow.core.leftIfNull
import no.nav.helse.Feilårsak
import no.nav.helse.arrow.sequenceU
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.person.PersonService
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.FinnBehandlendeEnhetListeUgyldigInput
import org.slf4j.LoggerFactory

class ArbeidsfordelingService(
        private val arbeidsfordelingClient: ArbeidsfordelingClient,
        private val personService: PersonService
) {
    companion object {
        private val log = LoggerFactory.getLogger(ArbeidsfordelingService::class.java)
    }

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

                    arbeidsfordelingClient.getBehandlendeEnhet(gjeldendeGeografiskeTilknytning, tema).toEither { err ->
                        log.error("Feil ved oppslag på behandlende enhet", err)

                        when (err) {
                            is FinnBehandlendeEnhetListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                            else -> Feilårsak.UkjentFeil
                        }
                    }.map {
                        BehandlendeEnhetMapper.tilEnhet(it)
                    }.leftIfNull {
                        Feilårsak.IkkeFunnet
                    }
                }
            }
}
