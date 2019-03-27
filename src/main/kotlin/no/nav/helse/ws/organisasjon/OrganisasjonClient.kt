package no.nav.helse.ws.organisasjon

import no.nav.helse.Either
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjon
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Organisasjonsfilter
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonRequest
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentVirksomhetsOrgnrForJuridiskOrgnrBolkRequest
import org.slf4j.LoggerFactory
import java.time.LocalDate

class OrganisasjonClient(private val organisasjonV5: OrganisasjonV5) {

    private val log = LoggerFactory.getLogger("OrganisasjonClient")

    fun hentOrganisasjon(orgnr: Organisasjonsnummer) : Either<Exception, Organisasjon> {
        val request = HentOrganisasjonRequest().apply { orgnummer = orgnr.value }
        return try {
            Either.Right(organisasjonV5.hentOrganisasjon(request).organisasjon)
        } catch (err : Exception) {
            log.error("Error during organisasjon lookup", err)
            Either.Left(err)
        }
    }

    fun hentVirksomhetForJuridiskOrganisasjonsnummer(orgnr: Organisasjonsnummer, dato: LocalDate = LocalDate.now()) =
            try {
                val request = HentVirksomhetsOrgnrForJuridiskOrgnrBolkRequest().apply {
                    with(organisasjonsfilterListe) {
                        add(Organisasjonsfilter().apply {
                            organisasjonsnummer = orgnr.value
                            hentingsdato = dato.toXmlGregorianCalendar()
                        })
                    }
                }
                Either.Right(organisasjonV5.hentVirksomhetsOrgnrForJuridiskOrgnrBolk(request))
            } catch (err: Exception) {
                log.error("Error during organisasjon lookup", err)
                Either.Left(err)
            }
}

data class Organisasjonsnummer(val value : String) {
    init {
        if (!Organisasjonsnummervalidator.erGyldig(value)) {
            throw IllegalArgumentException("Organisasjonsnummer $value er ugyldig")
        }
    }
}




