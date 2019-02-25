package no.nav.helse.ws.person

import no.nav.helse.ws.AktørId

class PersonService(private val personClient: PersonClient) {

    fun personInfo(aktørId: AktørId) = personClient.personInfo(aktørId)
    fun geografiskTilknytning(aktørId: AktørId) = personClient.geografiskTilknytning(aktørId)
}
