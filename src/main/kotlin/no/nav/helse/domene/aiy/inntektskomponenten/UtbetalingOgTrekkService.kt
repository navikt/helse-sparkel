package no.nav.helse.domene.aiy.inntektskomponenten

import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.AktørId
import no.nav.helse.oppslag.inntekt.InntektClient
import no.nav.helse.probe.DatakvalitetProbe
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.AktoerId
import org.slf4j.LoggerFactory
import java.time.YearMonth

class UtbetalingOgTrekkService(private val inntektClient: InntektClient,
                               private val datakvalitetProbe: DatakvalitetProbe) {

    companion object {
        private val log = LoggerFactory.getLogger(UtbetalingOgTrekkService::class.java)

        private val defaultFilter = "ForeldrepengerA-inntekt"
    }

    fun hentUtbetalingerOgTrekk(aktørId: AktørId, fom: YearMonth, tom: YearMonth, filter: String = defaultFilter) =
            inntektClient.hentInntekter(aktørId, fom, tom, filter)
                    .toEither(InntektskomponentenErrorMapper::mapToError)
                    .map { inntekterPerIdent ->
                        inntekterPerIdent.flatMap {
                            it.arbeidsInntektMaaned
                        }.flatMap {
                            it.arbeidsInntektInformasjon.inntektListe
                        }.onEach(datakvalitetProbe::tellInntektutbetaler)
                        .filter { inntekt ->
                            if (inntekt.inntektsmottaker is AktoerId) {
                                if ((inntekt.inntektsmottaker as AktoerId).aktoerId == aktørId.aktor) {
                                    true
                                } else {
                                    datakvalitetProbe.inntektGjelderEnAnnenAktør(inntekt)
                                    false
                                }
                            } else {
                                log.warn("inntektsmottaker er ikke en AktørId")
                                false
                            }
                        }.onEach(datakvalitetProbe::tellVirksomhetstypeForInntektutbetaler)
                        .filter { inntekt ->
                            if (fom.atDay(1) <= inntekt.utbetaltIPeriode.toLocalDate()
                                    && tom.atEndOfMonth() >= inntekt.utbetaltIPeriode.toLocalDate()) {
                                true
                            } else {
                                datakvalitetProbe.inntektErUtenforSøkeperiode(inntekt)
                                false
                            }
                        }
                        .map(UtbetalingEllerTrekkMapper::mapToUtbetalingEllerTrekk)
                        .filterNotNull()
                        .onEach { inntekt ->
                            datakvalitetProbe.inspiserUtbetalingEllerTrekk(inntekt)
                        }
                    }
}

