package no.nav.helse.ws.person

import no.nav.tjeneste.virksomhet.person.v3.binding.*
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*
import javax.xml.datatype.*

class PersonV3Stub: PersonV3 {
    override fun hentPerson(request: HentPersonRequest?): HentPersonResponse {
        return response()
    }

    private fun response(): HentPersonResponse {
        val mannen = Person().apply {
            personnavn = Personnavn().apply {
                fornavn = "Bjarne"
                etternavn = "Betjent"
                kjoenn = Kjoenn().apply {
                    kjoenn = Kjoennstyper().apply {
                        value = "M"
                    }
                }
            }
            foedselsdato = Foedselsdato().apply {
                foedselsdato = DatatypeFactory.newInstance().newXMLGregorianCalendar().apply {
                    year = 2018
                    month = 11
                    day = 19
                }
            }
        }

        return HentPersonResponse().apply { person = mannen }
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