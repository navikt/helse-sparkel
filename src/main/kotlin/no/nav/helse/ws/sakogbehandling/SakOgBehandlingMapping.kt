package no.nav.helse.ws.sakogbehandling

import no.nav.helse.common.*
import java.time.*


fun mapSak(sOgBSak: no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.finnsakogbehandlingskjedeliste.Sak): Sak {
    val sisteBehandling = sOgBSak.behandlingskjede?.let {
        it.asSequence()
          .sortedByDescending{ kjede -> kjede.slutt.toGregorianCalendar() }
          .firstOrNull()
    }

    return Sak(
            sOgBSak.saksId,
            sOgBSak.sakstema.value,
            sOgBSak.opprettet.toLocalDate(),
            sisteBehandling?.slutt?.toLocalDate(),
            sisteBehandling?.sisteBehandlingsstatus?.value)
}


data class Sak(
        val id: String,
        val tema: String,
        val opprettet: LocalDate,
        val sisteBehandlingSlutt: LocalDate? = null,
        val sisteStatus: String? = null)
