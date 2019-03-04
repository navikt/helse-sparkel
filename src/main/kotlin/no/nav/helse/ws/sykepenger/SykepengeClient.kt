package no.nav.helse.ws.sykepenger

import no.nav.helse.Either
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.sykepenger.v2.binding.SykepengerV2
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Periode
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Sykmeldingsperiode
import no.nav.tjeneste.virksomhet.sykepenger.v2.meldinger.HentSykepengerListeRequest
import org.slf4j.LoggerFactory
import java.time.LocalDate

class SykepengerClient(private val sykepenger: SykepengerV2) {

    private val log = LoggerFactory.getLogger("SykepengeClient")

    fun finnSykmeldingsperioder(fnr: Fødselsnummer, fraOgMed: LocalDate, tilOgMed: LocalDate): Either<Exception, List<Sykmeldingsperiode>> {
        val request = createSykepengerListeRequest(fnr.value, fraOgMed, tilOgMed)
        return try {
            Either.Right(sykepenger.hentSykepengerListe(request).sykmeldingsperiodeListe.toList())
        } catch (ex: Exception) {
            log.error("Error while doing sak og behndling lookup", ex)
            Either.Left(ex)
        }
    }

    fun createSykepengerListeRequest(fnr: String, fraOgMed: LocalDate, tilOgMed: LocalDate): HentSykepengerListeRequest {
        return HentSykepengerListeRequest()
                .apply { ident = fnr }
                .apply {
                    sykmelding = Periode()
                            .apply { fom = fraOgMed.toXmlGregorianCalendar() }
                            .apply { tom = tilOgMed.toXmlGregorianCalendar() }
                }
    }
}
