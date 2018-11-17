package no.nav.helse.ws.person

import no.nav.tjeneste.virksomhet.person.v3.binding.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*

class PersonV3MisbehavingStub: PersonV3 {
    override fun hentPerson(request: HentPersonRequest?): HentPersonResponse {
        throw Exception("SOAPy stuff got besmirched")
    }

    override fun ping() { TODO("not implemented") }

    override fun hentPersonnavnBolk(request: HentPersonnavnBolkRequest?): HentPersonnavnBolkResponse { TODO("not implemented") }

    override fun hentSikkerhetstiltak(request: HentSikkerhetstiltakRequest?): HentSikkerhetstiltakResponse { TODO("not implemented") }

    override fun hentGeografiskTilknytning(request: HentGeografiskTilknytningRequest?): HentGeografiskTilknytningResponse { TODO("not implemented") }

    override fun hentVerge(request: HentVergeRequest?): HentVergeResponse { TODO("not implemented") }

    override fun hentEkteskapshistorikk(request: HentEkteskapshistorikkRequest?): HentEkteskapshistorikkResponse { TODO("not implemented") }

    override fun hentPersonerMedSammeAdresse(request: HentPersonerMedSammeAdresseRequest?): HentPersonerMedSammeAdresseResponse { TODO("not implemented") }

    override fun hentPersonhistorikk(request: HentPersonhistorikkRequest?): HentPersonhistorikkResponse { TODO("not implemented") }

}