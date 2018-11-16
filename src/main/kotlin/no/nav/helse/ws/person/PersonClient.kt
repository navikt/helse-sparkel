package no.nav.helse.ws.person

import no.nav.helse.*
import no.nav.helse.ws.Fødselsnummer
import no.nav.helse.ws.person.Kjønn.*
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*
import org.slf4j.*


class PersonClient(private val personV3: PersonV3) {

    val log = LoggerFactory.getLogger(PersonClient::class.java)

    fun personInfo(id: Fødselsnummer): OppslagResult {
        val aktør = PersonIdent().apply {
            ident = NorskIdent().apply {
                ident = id.value
            }
        }

        val request = HentPersonRequest().apply {
            aktoer = aktør
        }

        val tpsPerson: HentPersonResponse?
        try {
            tpsPerson = personV3.hentPerson(request)
        } catch (ex: Exception) {
            log.error("Error during person lookup", ex)
            return Failure(listOf(ex.message ?: "unknown error"))
        }

        return Success(PersonMapper.toPerson(id, tpsPerson))

    }
}

object PersonMapper {
    fun toPerson(id: Fødselsnummer, response: HentPersonResponse): Person {
        val personnavn = response.person.personnavn
        return Person(
                id,
                personnavn.fornavn,
                personnavn.mellomnavn,
                personnavn.etternavn,
                if (response.person.kjoenn.kjoenn.value == "M") MANN else KVINNE
        )
    }
}

enum class Kjønn {
    MANN, KVINNE
}

data class Person(
        val id: Fødselsnummer,
        val fornavn: String,
        val mellomnavn: String? = null,
        val etternavn: String,
        val kjønn: Kjønn
)


