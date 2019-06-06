package no.nav.helse.domene.ytelse.infotrygd

import no.nav.helse.domene.ytelse.domain.*
import no.nav.helse.probe.InfluxMetricReporter
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.InfotrygdSak
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.InfotrygdVedtak
import java.util.*

class InfotrygdProbe(private val influxMetricReporter: InfluxMetricReporter) {

    enum class Observasjonstype {
        VerdiMangler,
        VerdiManglerIkke,
        TomVerdi,
        HarVerdi,
        UkjentTema,
        UtbetalingsgradMangler,
        HullIAnvistPeriode,
        AnvistPeriodeOk,
        FinnerIkkeGrunnlag,
        FinnerGrunnlag
    }

    fun inspiserInfotrygdSak(sak: InfotrygdSak) {
        val tema = Tema.fraKode(sak.tema.value)
        val type = if (sak is InfotrygdVedtak) {
            "Vedtak"
        } else {
            "Sak"
        }

        influxMetricReporter.sendDataPoint("infotrygd.saker",
                mapOf(
                        "uuid" to UUID.randomUUID().toString()
                ),
                mapOf(
                        "type" to type,
                        "tema" to sak.tema.value,
                        "behandlingstema" to sak.behandlingstema.value,
                        "sakstype" to (sak.type?.value ?: "IKKE_SATT").ifEmpty { "TOM_VERDI" },
                        "status" to sak.status.value.ifEmpty { "TOM_VERDI" },
                        "resultat" to (sak.resultat?.value ?: "IKKE_SATT").ifEmpty { "TOM_VERDI" },
                        "sakId" to (sak.sakId?.let { "HAR_VERDI" } ?: "IKKE_SATT").ifEmpty { "TOM_VERDI" },
                        "registrert" to (sak.registrert?.let { "HAR_VERDI" } ?: "IKKE_SATT"),
                        "vedtatt" to (sak.vedtatt?.let { "HAR_VERDI" } ?: "IKKE_SATT"),
                        "iverksatt" to (sak.iverksatt?.let { "HAR_VERDI" } ?: "IKKE_SATT"),
                        "opphoererFom" to if (sak is InfotrygdVedtak) (sak.opphoerFom?.let { "HAR_VERDI" } ?: "IKKE_SATT") else "IKKE_AKTUELT"
                ))

        sjekkOmFeltErNull(sak, "sakId ($tema)", sak.sakId)
        sak.sakId?.let {
            sjekkOmFeltErBlank(sak, "sakId ($tema)", it)
        }

        sjekkOmFeltErNull(sak, "registrert ($tema)", sak.registrert)

        sjekkOmFeltErNull(sak, "type ($tema)", sak.type?.value)
        sak.type?.value?.let {
            sjekkOmFeltErBlank(sak, "type ($tema)", it)
        }

        sjekkOmFeltErNull(sak, "status ($tema)", sak.status?.value)
        sak.status?.value?.let {
            sjekkOmFeltErBlank(sak, "status ($tema)", it)
        }

        sjekkOmFeltErNull(sak, "resultat ($tema)", sak.resultat?.value)
        sak.resultat?.value?.let {
            sjekkOmFeltErBlank(sak, "resultat ($tema)", it)
        }

        sjekkOmFeltErNull(sak, "iverksatt ($tema)", sak.iverksatt)
        sjekkOmFeltErNull(sak, "vedtatt ($tema)", sak.vedtatt)

        if (sak is InfotrygdVedtak) {
            sjekkOmFeltErNull(sak, "opphoerFom ($tema)", sak.opphoerFom)
        }
    }

    fun inspiserInfotrygdSakerOgGrunnlag(saker: List<InfotrygdSakOgGrunnlag>) {
        saker.forEach { sakMedGrunnlag ->
            inspiserSak(sakMedGrunnlag.sak)
            sakMedGrunnlag.grunnlag?.let(::inspiserGrunnlag)

            if (sakMedGrunnlag.grunnlag == null) {
                sendDatakvalitetEvent(sakMedGrunnlag, "grunnlag", Observasjonstype.FinnerIkkeGrunnlag, "finner ikke grunnlag for ${sakMedGrunnlag.sak}")
            } else {
                sendDatakvalitetEvent(sakMedGrunnlag, "grunnlag", Observasjonstype.FinnerGrunnlag, "finner grunnlag for ${sakMedGrunnlag.sak}")
            }
        }
    }

    private fun inspiserSak(sak: no.nav.helse.domene.ytelse.domain.InfotrygdSak) {
        if (sak.behandlingstema is Behandlingstema.Ukjent) {
            sendDatakvalitetEvent(sak, "behandlingstema", Observasjonstype.UkjentTema, "$sak har ukjent behandlingstema")
        }
        if (sak.tema is Tema.Ukjent) {
            sendDatakvalitetEvent(sak, "tema", Observasjonstype.UkjentTema, "$sak har ukjent tema")
        }

        if (sak is no.nav.helse.domene.ytelse.domain.InfotrygdSak.Vedtak) {
            sjekkOmFeltErNull(sak, "iverksatt", sak.iverksatt)
            sjekkOmFeltErNull(sak, "opphørerFom", sak.opphørerFom)
        }
    }

    private fun inspiserGrunnlag(grunnlag: Beregningsgrunnlag) {
        if (grunnlag.behandlingstema is Behandlingstema.Ukjent) {
            sendDatakvalitetEvent(grunnlag, "behandlingstema", Observasjonstype.UkjentTema, "$grunnlag har ukjent behandlingstema")
        }
        sjekkOmFeltErNull(grunnlag, "utbetalingFom", grunnlag.utbetalingFom)
        sjekkOmFeltErNull(grunnlag, "utbetalingTom", grunnlag.utbetalingTom)

        if (grunnlag.vedtak.isNotEmpty()) {
            try {
                if (grunnlag.vedtak[0].fom != grunnlag.utbetalingFom) {
                    throw IllegalArgumentException("første vedtak begynner på ${grunnlag.vedtak[0].fom}, perioden begynner på ${grunnlag.utbetalingFom}")
                }

                val last = grunnlag.vedtak.reduce { previous, current ->
                    if (previous.tom.plusDays(1) != current.fom) {
                        throw IllegalArgumentException("forrige vedtak slutter på ${previous.tom}, neste vedtak begynner på ${current.fom}")
                    }

                    current
                }

                if (last.tom != grunnlag.utbetalingTom) {
                    throw IllegalArgumentException("siste vedtak slutter på ${last.tom}, perioden slutter på ${grunnlag.utbetalingTom}")
                }

                sendDatakvalitetEvent(grunnlag, "vedtak", Observasjonstype.AnvistPeriodeOk, "$grunnlag har en ok anvist periode")
            } catch (err: IllegalArgumentException) {
                sendDatakvalitetEvent(grunnlag, "vedtak", Observasjonstype.HullIAnvistPeriode, "$grunnlag har hull i anvist periode: ${err.message}")
            }

            if (grunnlag.vedtak.any { it is Utbetalingsvedtak.SkalIkkeUtbetales }) {
                sendDatakvalitetEvent(grunnlag, "vedtak", Observasjonstype.UtbetalingsgradMangler, "$grunnlag har ${grunnlag.vedtak.filter { it is Utbetalingsvedtak.SkalIkkeUtbetales }.size} vedtak med manglende utbetalingsgrad")
            }
        }
    }

    private fun sendDatakvalitetEvent(objekt: Any, felt: String, observasjonstype: Observasjonstype, beskrivelse: String) {
        influxMetricReporter.sendDataPoint("datakvalitet.event",
                mapOf(
                        "beskrivelse" to beskrivelse
                ),
                mapOf(
                        "objekt" to objekt.javaClass.name,
                        "felt" to felt,
                        "type" to observasjonstype.name
                ))
    }

    private fun sjekkOmFeltErNull(objekt: Any, felt: String, value: Any?) {
        if (value == null) {
            sendDatakvalitetEvent(objekt, felt, Observasjonstype.VerdiMangler, "felt manger: $felt er null")
        } else {
            sendDatakvalitetEvent(objekt, felt, Observasjonstype.VerdiManglerIkke, "$felt er ikke null")
        }
    }

    private fun sjekkOmFeltErBlank(objekt: Any, felt: String, value: String) {
        if (value.isBlank()) {
            sendDatakvalitetEvent(objekt, felt, Observasjonstype.TomVerdi, "tomt felt: $felt er tom, eller består bare av whitespace")
        } else {
            sendDatakvalitetEvent(objekt, felt, Observasjonstype.HarVerdi, "$felt har verdi")
        }
    }
}
