package no.nav.helse.ws.person

import no.nav.helse.ws.*
import no.nav.helse.ws.person.Kjønn.*
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*
import java.time.*
import javax.xml.datatype.*

private val datatypeFactory = DatatypeFactory.newInstance()

object PersonMapper {
    fun toPerson(response: HentPersonResponse): Person {
        val tpsPerson = response.person

        return Person(
                AktørId((tpsPerson.aktoer as AktoerId).aktoerId),
                tpsPerson.personnavn.fornavn,
                tpsPerson.personnavn.mellomnavn,
                tpsPerson.personnavn.etternavn,
                tpsPerson.foedselsdato.foedselsdato.toLocalDate(),
                if (response.person.kjoenn.kjoenn.value == "M") MANN else KVINNE
        )
    }
}

object PersonhistorikkMapper {
    fun toPersonhistorikk(response: HentPersonhistorikkResponse) =
        Personhistorikk(
                AktørId((response.aktoer as AktoerId).aktoerId),
                response.statsborgerskapListe.map(::statsborgerskapPeriode),
                response.personstatusListe.map(::statusPeriode),
                response.bostedsadressePeriodeListe.map(::bostedsPeriode)
        )
}

enum class Kjønn {
    MANN, KVINNE
}

private fun statsborgerskapPeriode(periode: StatsborgerskapPeriode): TidsperiodeMedVerdi {
    return TidsperiodeMedVerdi(
            periode.statsborgerskap?.land?.value ?: "ukjent land",
                periode.periode.fom.toLocalDate(),
                periode.periode.tom.toLocalDate()
    )
}

private fun statusPeriode(periode: PersonstatusPeriode): TidsperiodeMedVerdi {
    return TidsperiodeMedVerdi(
            periode.personstatus?.value ?: "ukjent status",
            periode.periode.fom.toLocalDate(),
            periode.periode.tom.toLocalDate()
    )
}

private fun bostedsPeriode(periode: BostedsadressePeriode): TidsperiodeMedVerdi {
    return TidsperiodeMedVerdi(
            bostedsAdresse(periode.bostedsadresse),
            periode.periode.fom.toLocalDate(),
            periode.periode.tom.toLocalDate()
    )
}

private fun bostedsAdresse(bosted: Bostedsadresse): String {
    return when (val strukturertAdresse = bosted.strukturertAdresse) {
        is Stedsadresse -> stedsadresse(strukturertAdresse)
        is Postboksadresse -> postboksadresse(strukturertAdresse)
        else -> "ukjent adressetype ${strukturertAdresse.javaClass}"
    }
}

private fun stedsadresse(adr: Stedsadresse): String =
        when (adr) {
            is Gateadresse -> gateadresse(adr)
            is Matrikkeladresse -> matrikkeladresse(adr)
            else -> "ukjent adressetype ${adr.javaClass}"
        }

private fun gateadresse(adr: Gateadresse): String =
        "${adr.gatenavn} ${adr.husnummer}${adr.husbokstav ?: ""}, ${adr.poststed.value}"

private fun matrikkeladresse(adr: Matrikkeladresse): String =
    "${adr.eiendomsnavn ?: ""}, ${adr.matrikkelnummer.gaardsnummer}/${adr.matrikkelnummer.bruksnummer}, ${adr.poststed.value}"

private fun postboksadresse(adr: Postboksadresse) =
    (adr as PostboksadresseNorsk).let {
        "${it.postboksanlegg}, ${it.poststed?.value}"
    }

data class Person(
        val id: AktørId,
        val fornavn: String,
        val mellomnavn: String? = null,
        val etternavn: String,
        val fdato: LocalDate,
        val kjønn: Kjønn
)

data class Personhistorikk(
        val id: AktørId,
        val statsborgerskap: List<TidsperiodeMedVerdi>,
        val statuser: List<TidsperiodeMedVerdi>,
        val bostedsadresser: List<TidsperiodeMedVerdi>
)

data class TidsperiodeMedVerdi(val verdi: String, val fom: LocalDate, val tom: LocalDate) {

    init {
        if (tom.isBefore(fom)) throw IllegalArgumentException("tom cannot be before fom, $tom is before $fom")
    }

}

fun XMLGregorianCalendar.toLocalDate() = LocalDate.of(year, month, day)

fun LocalDate.toXmlGregorianCalendar() = this.let { localDate ->
    datatypeFactory.newXMLGregorianCalendar().apply {
        year = localDate.year
        month = localDate.monthValue
        day = localDate.dayOfMonth
    }
}