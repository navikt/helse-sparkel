package no.nav.helse.ws.sykepenger

import no.nav.helse.Either
import no.nav.helse.Feilårsak
import no.nav.helse.bimap
import no.nav.helse.common.toLocalDate
import no.nav.helse.flatMap
import no.nav.helse.http.aktør.AktørregisterService
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Sykmeldingsperiode
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Vedtak
import no.nav.tjeneste.virksomhet.sykepenger.v2.meldinger.HentSykepengerListeResponse
import java.math.BigDecimal
import java.time.LocalDate

class SykepengelisteService(private val hentSykepengeperiodeClient: HentSykepengeListeRestClient, private val aktørregisterService: AktørregisterService) {

    fun finnSykepengeperioder(aktørId: AktørId): Either<Feilårsak, List<SykepengerPeriode>> =
            aktørregisterService.fødselsnummerForAktør(aktørId).flatMap { fnr ->
                hentSykepengeperiodeClient.hentSykepengeListe(fnr).bimap({
                    Feilårsak.FeilFraTjeneste
                }, {
                    it
                })
            }
}
