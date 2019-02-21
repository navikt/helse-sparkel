package no.nav.helse.ws.arbeidsfordeling

import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.person.GeografiskTilknytning
import no.nav.helse.ws.person.PersonService
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("ArbeidsfordelingService")

class ArbeidsfordelingService(
        private val arbeidsfordelingClient: ArbeidsfordelingClient,
        private val personService: PersonService
) {
    fun getBehandlendeEnhet(
            hovedAktoer: AktørId,
            medAktoerer: List<AktørId> = listOf(),
            tema: Tema
    ) : OppslagResult<Feil, Enhet> {
        // Geografisk tilknytning for Hovedaktør
        val geografiskTilknytningHovedAktoerOppslagResponse = personService.geografiskTilknytning(hovedAktoer)
        if (geografiskTilknytningHovedAktoerOppslagResponse is OppslagResult.Feil) {
            return geografiskTilknytningHovedAktoerOppslagResponse
        }
        val geografiskTilknytningHovedAktoer = (geografiskTilknytningHovedAktoerOppslagResponse as OppslagResult.Ok).data

        // Geografisk tilknytning for eventuelle Medaktører
        val geografiskTilknytningMedAktoerer = mutableListOf<GeografiskTilknytning>()
        medAktoerer.forEach { medAktoer ->
            val geografiskTilknytningMedAktoerOppslagResponse = personService.geografiskTilknytning(medAktoer)
            if (geografiskTilknytningMedAktoerOppslagResponse is OppslagResult.Feil) {
                return geografiskTilknytningMedAktoerOppslagResponse
            }
            val geografiskTilknytningMedAktoer = (geografiskTilknytningMedAktoerOppslagResponse as OppslagResult.Ok).data
            geografiskTilknytningMedAktoerer.add(geografiskTilknytningMedAktoer)
        }

        // Gjeldende geografiske tilknytning
        val gjeldendeGeografiskeTilknytning = velgGjeldendeGeografiskeTilknytning(
                forHovedAktoer = geografiskTilknytningHovedAktoer,
                forMedAktoerer = geografiskTilknytningMedAktoerer.toList()
        )
        // Gjør oppslag for å finne enhet
        return arbeidsfordelingClient.getBehandlendeEnhet(gjeldendeGeografiskeTilknytning, tema)
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
