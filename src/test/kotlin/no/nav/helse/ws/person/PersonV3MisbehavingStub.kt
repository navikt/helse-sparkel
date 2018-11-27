package no.nav.helse.ws.person

import no.nav.tjeneste.virksomhet.person.v3.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*

class PersonV3MisbehavingStub: PersonV3 {
    override fun hentPerson(request: WSHentPersonRequest?): WSHentPersonResponse {
        throw Exception("SOAPy stuff got besmirched")
    }

    override fun ping() { TODO("not implemented") }

    override fun hentPersonnavnBolk(request: WSHentPersonnavnBolkRequest?): WSHentPersonnavnBolkResponse { TODO("not implemented") }

    override fun hentSikkerhetstiltak(request: WSHentSikkerhetstiltakRequest?): WSHentSikkerhetstiltakResponse { TODO("not implemented") }

    override fun hentGeografiskTilknytning(request: WSHentGeografiskTilknytningRequest?): WSHentGeografiskTilknytningResponse { TODO("not implemented") }

    override fun hentVerge(request: WSHentVergeRequest?): WSHentVergeResponse { TODO("not implemented") }
}
