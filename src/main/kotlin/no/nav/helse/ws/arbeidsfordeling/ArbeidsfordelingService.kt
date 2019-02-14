package no.nav.helse.ws.arbeidsfordeling

import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.person.GeografiskTilknytning
import no.nav.helse.ws.person.PersonClient
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("ArbeidsfordelingService")

class ArbeidsfordelingService(
        val arbeidsfordelingClient: ArbeidsfordelingClient,
        val personClient: PersonClient
) {
    fun getBehandlendeEnhet(
            hovedAktoer: AktørId,
            medAktoerer: List<AktørId> = listOf(),
            tema: Tema
    ) : OppslagResult {
        // Geografisk tilknytning for Hovedaktør
        val geografiskTilknytningHovedAktoerOppslagResponse = personClient.geografiskTilknytning(hovedAktoer)
        if (geografiskTilknytningHovedAktoerOppslagResponse is Failure) return geografiskTilknytningHovedAktoerOppslagResponse
        val geografiskTilknytningHovedAktoer = (geografiskTilknytningHovedAktoerOppslagResponse as Success<*>).data as GeografiskTilknytning

        // Geografisk tilknytning for eventuelle Medaktører
        val geografiskTilknytningMedAktoerer = mutableListOf<GeografiskTilknytning>()
        medAktoerer.forEach { medAktoer ->
            val geografiskTilknytningMedAktoerOppslagResponse = personClient.geografiskTilknytning(medAktoer)
            if (geografiskTilknytningMedAktoerOppslagResponse is Failure) return geografiskTilknytningMedAktoerOppslagResponse
            val geografiskTilknytningMedAktoer = (geografiskTilknytningMedAktoerOppslagResponse as Success<*>).data as GeografiskTilknytning
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