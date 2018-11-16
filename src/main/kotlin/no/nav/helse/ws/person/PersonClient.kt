package no.nav.helse.ws.person

import io.prometheus.client.*
import no.nav.helse.*
import no.nav.helse.ws.person.Kjønn.*
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*

class PersonClient(private val personV3: PersonV3) {

    private val counter = Counter.build()
            .name("oppslag_person")
            .labelNames("status")
            .help("Antall registeroppslag av personer")
            .register()

    fun personInfo(id: Fødselsnummer): OppslagResult {
        val aktør = PersonIdent().apply {
            ident = NorskIdent().apply {
                ident = id.value
            }
        }

        val request = HentPersonRequest().apply {
            aktoer = aktør
        }

        return try {
            val tpsResponse = personV3.hentPerson(request)
            counter.labels("success").inc()
            Success(PersonMapper.toPerson(id, tpsResponse))
        } catch (ex: Exception) {
            counter.labels("failure").inc()
            Failure(listOf(ex.message ?: "unknown error"))
        }

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

data class Fødselsnummer(val value: String) {
    val elevenDigits = Regex("\\d{11}")

    init {
        if (!elevenDigits.matches(value)) {
            throw IllegalArgumentException("$value is not a valid fnr")
        }
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


