package no.nav.helse.ws.person

import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Periode
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkRequest
import org.slf4j.LoggerFactory
import java.time.LocalDate

class PersonClient(private val personV3: PersonV3) {

    private val log = LoggerFactory.getLogger("PersonClient")

    fun personInfo(id: AktørId): OppslagResult {
        val request = HentPersonRequest().apply {
            aktoer = AktoerId().apply {
                aktoerId = id.aktor
            }
        }

        return try {
            val tpsResponse = personV3.hentPerson(request)
            Success(PersonMapper.toPerson(tpsResponse))
        } catch (ex: Exception) {
            log.error("Error while doing person lookup", ex)
            Failure(listOf(ex.message ?: "unknown error"))
        }
    }

    fun personHistorikk(id: AktørId, fom: LocalDate, tom: LocalDate): OppslagResult {
        val request = HentPersonhistorikkRequest().apply {
            aktoer = AktoerId().apply {
                aktoerId = id.aktor
            }

            periode = Periode().apply {
                this.fom = fom.toXmlGregorianCalendar()
                this.tom = tom.toXmlGregorianCalendar()
            }
        }

        return try {
            val tpsResponse = personV3.hentPersonhistorikk(request)
            Success(PersonhistorikkMapper.toPersonhistorikk(tpsResponse))
        } catch (ex: Exception) {
            log.error("Error while doing personhistorikk lookup", ex)
            Failure(listOf(ex.message ?: "unknown error"))
        }
    }

    fun geografiskTilknytning(id : AktørId) : OppslagResult {
        val request = HentGeografiskTilknytningRequest().apply {
            aktoer = AktoerId().apply {
                aktoerId = id.aktor
            }
        }

        return try {
            val tpsResponse = personV3.hentGeografiskTilknytning(request)
            Success(GeografiskTilknytningMapper.tilGeografiskTilknytning(tpsResponse))
        } catch (cause: Throwable) {
            log.error("Error while doing geografisk tilknytning lookup", cause)
            Failure(listOf(cause.message ?: "unknown error"))
        }
    }
}







