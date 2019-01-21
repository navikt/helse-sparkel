package no.nav.helse.ws.person

import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentEkteskapshistorikkRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentEkteskapshistorikkResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonerMedSammeAdresseRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonerMedSammeAdresseResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonnavnBolkRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonnavnBolkResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentSikkerhetstiltakRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentSikkerhetstiltakResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentVergeRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentVergeResponse
import javax.xml.datatype.DatatypeFactory

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
            aktoer = AktoerId().apply {
                aktoerId = "1234567891011"
            }
        }

        return HentPersonResponse().apply { person = mannen }
    }

    override fun hentPersonhistorikk(request: HentPersonhistorikkRequest?): HentPersonhistorikkResponse { TODO("not implemented") }

    override fun hentEkteskapshistorikk(request: HentEkteskapshistorikkRequest?): HentEkteskapshistorikkResponse { TODO("not implemented") }

    override fun hentPersonerMedSammeAdresse(request: HentPersonerMedSammeAdresseRequest?): HentPersonerMedSammeAdresseResponse { TODO("not implemented") }

    override fun ping() { TODO("not implemented") }

    override fun hentPersonnavnBolk(request: HentPersonnavnBolkRequest?): HentPersonnavnBolkResponse { TODO("not implemented") }

    override fun hentSikkerhetstiltak(request: HentSikkerhetstiltakRequest?): HentSikkerhetstiltakResponse { TODO("not implemented") }

    override fun hentGeografiskTilknytning(request: HentGeografiskTilknytningRequest?): HentGeografiskTilknytningResponse { TODO("not implemented") }

    override fun hentVerge(request: HentVergeRequest?): HentVergeResponse { TODO("not implemented") }

}
