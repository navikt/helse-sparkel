package no.nav.helse.ws.sykepenger

import arrow.core.Try
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.sykepenger.v2.binding.SykepengerV2
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Periode
import no.nav.tjeneste.virksomhet.sykepenger.v2.meldinger.HentSykepengerListeRequest
import java.time.LocalDate

class SykepengerClient(private val sykepenger: SykepengerV2) {

    fun finnSykmeldingsperioder(fnr: Fødselsnummer, fraOgMed: LocalDate, tilOgMed: LocalDate) =
            Try {
                sykepenger.hentSykepengerListe(createSykepengerListeRequest(fnr.value, fraOgMed, tilOgMed)).sykmeldingsperiodeListe.toList()
            }

    internal fun createSykepengerListeRequest(fnr: String, fraOgMed: LocalDate, tilOgMed: LocalDate): HentSykepengerListeRequest {
        return HentSykepengerListeRequest()
                .apply { ident = fnr }
                .apply {
                    sykmelding = Periode()
                            .apply { fom = fraOgMed.toXmlGregorianCalendar() }
                            .apply { tom = tilOgMed.toXmlGregorianCalendar() }
                }
    }
}
