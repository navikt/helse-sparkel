package no.nav.helse.ws.sykepenger

import no.nav.helse.Either
import no.nav.helse.Feilårsak
import no.nav.helse.bimap
import no.nav.helse.common.toLocalDate
import no.nav.helse.flatMap
import no.nav.helse.http.aktør.AktørregisterService
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.sykepenger.v2.binding.HentSykepengerListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Sykmeldingsperiode
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Vedtak
import no.nav.tjeneste.virksomhet.sykepenger.v2.meldinger.HentSykepengerListeResponse
import java.math.BigDecimal
import java.time.LocalDate

class SykepengelisteService(private val sykepengerClient: SykepengerClient, private val hentSykepengeperiodeClient: HentSykepengeListeRestClient, private val aktørregisterService: AktørregisterService) {

    fun finnSykepengevedtak(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            aktørregisterService.fødselsnummerForAktør(aktørId).flatMap { fnr ->
                sykepengerClient.finnSykmeldingsperioder(Fødselsnummer(fnr), fom, tom).bimap({
                    when (it) {
                        is HentSykepengerListeSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                        else -> Feilårsak.UkjentFeil
                    }
                }, { sykmeldingsperioder ->
                    sykmeldingsperioder.flatMap { periode ->
                        periode.toSykepengerVedtak(fnr)
                    }
                })
            }

    fun finnSykepengeperioder(aktørId: AktørId): Either<Feilårsak, List<SykepengerPeriode>> =
            aktørregisterService.fødselsnummerForAktør(aktørId).flatMap { fnr ->
                hentSykepengeperiodeClient.hentSykepengeListe(fnr).bimap({
                    Feilårsak.FeilFraTjeneste
                }, {
                    it
                })
            }
}

data class SykepengerVedtak(val fom: LocalDate,
                            val tom: LocalDate,
                            val grad: Float,
                            val dagsats: BigDecimal = BigDecimal.ZERO)

fun HentSykepengerListeResponse.toSykepengerVedtak(fnr: String): Collection<SykepengerVedtak> {
    return when {
        this.sykmeldingsperiodeListe.isEmpty() -> emptyList()
        else -> this.sykmeldingsperiodeListe.flatMap { it.toSykepengerVedtak(fnr) }
    }
}

fun Sykmeldingsperiode.toSykepengerVedtak(fnr: String): Collection<SykepengerVedtak> {
    return when {
        this.vedtakListe.isEmpty() -> emptyList()
        else -> this.vedtakListe.map { it.toSykepengerVedtak(fnr) }
    }
}

fun Vedtak.toSykepengerVedtak(fnr: String): SykepengerVedtak {
    return SykepengerVedtak(fom = this.vedtak.fom.toLocalDate(),
            tom = this.vedtak.tom.toLocalDate(),
            dagsats = BigDecimal.ONE,
            grad = this.utbetalingsgrad.toFloat())
}

