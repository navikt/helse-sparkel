package no.nav.helse.ws.person

import no.nav.tjeneste.virksomhet.person.v3.*
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*
import javax.xml.datatype.*

class PersonV3Stub: PersonV3 {
    override fun hentPerson(request: WSHentPersonRequest?): WSHentPersonResponse {
        return response()
    }

    private fun response(): WSHentPersonResponse {
        val mannen = WSPerson().apply {
            personnavn = WSPersonnavn().apply {
                fornavn = "Bjarne"
                etternavn = "Betjent"
                kjoenn = WSKjoenn().apply {
                    kjoenn = WSKjoennstyper().apply {
                        value = "M"
                    }
                }
            }
            foedselsdato = WSFoedselsdato().apply {
                foedselsdato = DatatypeFactory.newInstance().newXMLGregorianCalendar().apply {
                    year = 2018
                    month = 11
                    day = 19
                }
            }
        }

        return WSHentPersonResponse().apply { person = mannen }
    }

    override fun ping() { TODO("not implemented") }

    override fun hentPersonnavnBolk(request: WSHentPersonnavnBolkRequest?): WSHentPersonnavnBolkResponse { TODO("not implemented") }

    override fun hentSikkerhetstiltak(request: WSHentSikkerhetstiltakRequest?): WSHentSikkerhetstiltakResponse { TODO("not implemented") }

    override fun hentGeografiskTilknytning(request: WSHentGeografiskTilknytningRequest?): WSHentGeografiskTilknytningResponse { TODO("not implemented") }

    override fun hentVerge(request: WSHentVergeRequest?): WSHentVergeResponse { TODO("not implemented") }

}
