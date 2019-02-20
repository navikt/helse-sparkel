package no.nav.helse.ws.person

import no.nav.helse.ws.AktørId
import java.time.LocalDate

class PersonService(private val personClient: PersonClient) {

    fun personInfo(aktørId: AktørId) = personClient.personInfo(aktørId)
    fun personHistorikk(aktørId: AktørId) = personClient.personHistorikk(aktørId, LocalDate.now().minusYears(3), LocalDate.now())
    fun geografiskTilknytning(aktørId: AktørId) = personClient.geografiskTilknytning(aktørId)
}
