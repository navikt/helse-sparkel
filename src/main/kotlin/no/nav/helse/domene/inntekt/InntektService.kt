package no.nav.helse.domene.inntekt

import no.nav.helse.Feilårsak
import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.inntekt.InntektMapper.mapToIntekt
import no.nav.helse.oppslag.inntekt.InntektClient
import no.nav.helse.oppslag.inntekt.SikkerhetsavvikException
import no.nav.helse.probe.DatakvalitetProbe
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkUgyldigInput
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.AktoerId
import org.slf4j.LoggerFactory
import java.time.YearMonth

class InntektService(private val inntektClient: InntektClient,
                     private val datakvalitetProbe: DatakvalitetProbe) {

    companion object {
        private val log = LoggerFactory.getLogger(InntektService::class.java)

        private val defaultFilter = "ForeldrepengerA-inntekt"
    }

    fun hentInntekter(aktørId: AktørId, fom: YearMonth, tom: YearMonth, filter: String = defaultFilter) =
            inntektClient.hentInntekter(aktørId, fom, tom, filter).toEither { err ->
                log.error("Error during inntekt lookup", err)

                when (err) {
                    is HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter -> Feilårsak.FeilFraTjeneste
                    is HentInntektListeBolkUgyldigInput -> Feilårsak.FeilFraTjeneste
                    is SikkerhetsavvikException -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.UkjentFeil
                }
            }.map { inntekterPerIdent ->
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
                .map(InntektMapper::mapToIntekt)
                .filterNotNull()
                .onEach { inntekt ->
                    datakvalitetProbe.inspiserInntekt(inntekt)
                }
            }
}

