package no.nav.helse.domene.ytelse.sykepengehistorikk

import arrow.core.flatMap
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.ytelse.domain.Beregningsgrunnlag
import no.nav.helse.domene.ytelse.domain.Utbetalingsvedtak
import no.nav.helse.domene.ytelse.infotrygd.InfotrygdService
import java.time.LocalDate


class SykepengehistorikkService(private val infotrygdService: InfotrygdService,
                                private val aktørregisterService: AktørregisterService) {

    fun hentSykepengeHistorikk(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            aktørregisterService.fødselsnummerForAktør(aktørId).flatMap { fnr ->
                infotrygdService.finnGrunnlag(Fødselsnummer(fnr), fom, tom).map { response ->
                    response.filter {
                        it is Beregningsgrunnlag.Sykepenger
                    }.flatMap {
                        it.vedtak
                    }.filter {
                        it is Utbetalingsvedtak.SkalUtbetales && it.utbetalingsgrad > 0
                    }
                }
            }
}
