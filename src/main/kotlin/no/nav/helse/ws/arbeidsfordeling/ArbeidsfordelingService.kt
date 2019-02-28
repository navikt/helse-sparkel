package no.nav.helse.ws.arbeidsfordeling

import no.nav.helse.Either
import no.nav.helse.Feilårsak
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.person.GeografiskTilknytning
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
    ) : Either<Feilårsak, Enhet> {
        // Geografisk tilknytning for Hovedaktør
        val geografiskTilknytningHovedAktoerOppslagResponse = personService.geografiskTilknytning(hovedAktoer)
        if (geografiskTilknytningHovedAktoerOppslagResponse is Either.Left) {
            return geografiskTilknytningHovedAktoerOppslagResponse
        }
        val geografiskTilknytningHovedAktoer = (geografiskTilknytningHovedAktoerOppslagResponse as Either.Right).right

        // Geografisk tilknytning for eventuelle Medaktører
        val geografiskTilknytningMedAktoerer = mutableListOf<GeografiskTilknytning>()
        medAktoerer.forEach { medAktoer ->
            val geografiskTilknytningMedAktoerOppslagResponse = personService.geografiskTilknytning(medAktoer)
            if (geografiskTilknytningMedAktoerOppslagResponse is Either.Left) {
                return geografiskTilknytningMedAktoerOppslagResponse
            }
            val geografiskTilknytningMedAktoer = (geografiskTilknytningMedAktoerOppslagResponse as Either.Right).right
            geografiskTilknytningMedAktoerer.add(geografiskTilknytningMedAktoer)
        }

        // Gjeldende geografiske tilknytning
        val gjeldendeGeografiskeTilknytning = velgGjeldendeGeografiskeTilknytning(
                forHovedAktoer = geografiskTilknytningHovedAktoer,
                forMedAktoerer = geografiskTilknytningMedAktoerer.toList()
        )

        val result = arbeidsfordelingClient.getBehandlendeEnhet(gjeldendeGeografiskeTilknytning, tema)
        return when (result) {
            is Either.Right -> result
            is Either.Left -> {
                Either.Left(when (result.left) {
                    is IngenEnhetFunnetException -> Feilårsak.IkkeFunnet
                    is FinnBehandlendeEnhetListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.UkjentFeil
                })
            }
        }
    }

    private fun velgGjeldendeGeografiskeTilknytning(
            forHovedAktoer: GeografiskTilknytning,
            forMedAktoerer: List<GeografiskTilknytning>
    ) : GeografiskTilknytning {
        // Om noen er kode 6 brukes denne geograffiske tilknytningen
        forMedAktoerer.forEach {
            if (it.diskresjonskode?.kode == 6) return it
        }
        // Om ingen medaktører har kode 6 brukes alltid hovedaktør
        return forHovedAktoer
    }
}
