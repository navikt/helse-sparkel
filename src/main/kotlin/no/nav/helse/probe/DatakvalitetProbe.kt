package no.nav.helse.probe

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.domene.arbeid.domain.Arbeidsavtale
import no.nav.helse.domene.arbeid.domain.Arbeidsforhold
import no.nav.helse.domene.arbeid.domain.Permisjon
import no.nav.helse.domene.inntekt.domain.Inntekt
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate

class DatakvalitetProbe(sensuClient: SensuClient) {

    private val influxMetricReporter = InfluxMetricReporter(sensuClient, "sparkel-events", mapOf(
            "application" to (System.getenv("NAIS_APP_NAME") ?: "sparkel"),
            "cluster" to (System.getenv("NAIS_CLUSTER_NAME") ?: "dev-fss"),
            "namespace" to (System.getenv("NAIS_NAMESPACE") ?: "default")
    ))

    companion object {
        private val log = LoggerFactory.getLogger(DatakvalitetProbe::class.java)

        private val arbeidsforholdHistogram = Histogram.build()
                .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 15.0, 20.0, 40.0, 60.0, 80.0, 100.0)
                .name("arbeidsforhold_sizes")
                .help("fordeling over hvor mange arbeidsforhold en arbeidstaker har, både frilans og vanlig arbeidstaker")
                .register()

        private val frilansCounter = Counter.build()
                .name("arbeidsforhold_frilans_totals")
                .help("antall frilans arbeidsforhold")
                .register()

        private val arbeidsforholdISammeVirksomhetCounter = Counter.build()
                .name("arbeidsforhold_i_samme_virksomhet_totals")
                .help("antall arbeidsforhold i samme virksomhet")
                .register()

        private val arbeidsforholdAvviksCounter = Counter.build()
                .name("arbeidsforhold_avvik_totals")
                .labelNames("type")
                .help("antall arbeidsforhold som ikke har noen tilhørende inntekter")
                .register()

        private val inntektAvviksCounter = Counter.build()
                .name("inntekt_avvik_totals")
                .help("antall inntekter som ikke har noen tilhørende arbeidsforhold")
                .register()

        private val arbeidsforholdPerInntektHistogram = Histogram.build()
                .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0)
                .name("arbeidsforhold_per_inntekt_sizes")
                .help("fordeling over hvor mange potensielle arbeidsforhold en inntekt har")
                .register()

        private val inntektCounter = Counter.build()
                .name("inntekt_totals")
                .help("antall inntekter mottatt, fordelt på inntektstype")
                .labelNames("type")
                .register()
        private val andreAktørerCounter = Counter.build()
                .name("inntekt_andre_aktorer_totals")
                .help("antall inntekter mottatt med andre aktører enn den det ble gjort oppslag på")
                .register()
        private val inntekterUtenforPeriodeCounter = Counter.build()
                .name("inntekt_utenfor_periode_totals")
                .help("antall inntekter med periode (enten opptjeningsperiode eller utbetaltIPeriode) utenfor søkeperioden")
                .register()
        private val inntektArbeidsgivertypeCounter = Counter.build()
                .name("inntekt_arbeidsgivertype_totals")
                .labelNames("type")
                .help("antall inntekter fordelt på ulike arbeidsgivertyper")
                .register()
    }

    enum class Observasjonstype {
        ErStørreEnnHundre,
        FraOgMedDatoErStørreEnnTilOgMedDato,
        StartdatoStørreEnnSluttdato,
        DatoErIFremtiden,
        VerdiMangler,
        TomVerdi,
        TomListe,
        FlereGjeldendeArbeidsavtaler,
        FlereArbeidsforholdPerInntekt,
        ArbeidsforholdISammeVirksomhet,
        ErMindreEnnNull,
        InntektGjelderEnAnnenAktør
    }

    fun inspiserArbeidstaker(arbeidsforhold: Arbeidsforhold.Arbeidstaker) {
        sjekkOmStartdatoErStørreEnnSluttdato(arbeidsforhold, "startdato,sluttdato", arbeidsforhold.startdato, arbeidsforhold.sluttdato)
        sjekkOmDatoErIFremtiden(arbeidsforhold, "sluttdato", arbeidsforhold.sluttdato)

        arbeidsforhold.permisjon.forEach { permisjon ->
            inspiserPermisjon(permisjon)
        }

        sjekkOmListeErTom(arbeidsforhold, "arbeidsavtaler", arbeidsforhold.arbeidsavtaler)

        val gjeldendeArbeidsavtaler = arbeidsforhold.arbeidsavtaler.filter { it.tom == null }.size
        if (gjeldendeArbeidsavtaler > 1) {
            flereGjeldendeArbeidsavtaler(arbeidsforhold, "arbeidsavtaler", gjeldendeArbeidsavtaler)
        }

        arbeidsforhold.arbeidsavtaler.forEach { arbeidsavtale ->
            inspiserArbeidsavtale(arbeidsavtale)
        }
    }

    fun inspiserInntekt(inntekt: Inntekt) {
        if (inntekt.beløp < BigDecimal.ZERO) {
            beløpErMindreEnnNull(inntekt, "beløp", inntekt.beløp)
        }
    }

    fun inspiserArbeidInntektYtelse(arbeidInntektYtelse: ArbeidInntektYtelse) {
        arbeidsforholdHistogram.observe(arbeidInntektYtelse.arbeidsforhold.size.toDouble())

        val unikeArbeidsgivere = arbeidInntektYtelse.arbeidsforhold.distinctBy {
            it.arbeidsgiver
        }
        val arbeidsforholdISammeVirksomhet = arbeidInntektYtelse.arbeidsforhold.size - unikeArbeidsgivere.size

        if (arbeidsforholdISammeVirksomhet > 0) {
            arbeidsforholdISammeVirksomhetCounter.inc(arbeidsforholdISammeVirksomhet.toDouble())
        }

        arbeidInntektYtelse.lønnsinntekter.forEach { inntekt ->
            if (inntekt.second.size > 1) {
                sendDatakvalitetEvent(arbeidInntektYtelse, "lønnsinntekter", Observasjonstype.FlereArbeidsforholdPerInntekt, "det er ${inntekt.second.size} potensielle arbeidsforhold for inntekt")
            }
        }
    }

    fun inspiserFrilans(arbeidsforhold: Arbeidsforhold.Frilans) {
        sjekkOmStartdatoErStørreEnnSluttdato(arbeidsforhold, "startdato,sluttdato", arbeidsforhold.startdato, arbeidsforhold.sluttdato)
        sjekkOmFeltErBlank(arbeidsforhold, "yrke", arbeidsforhold.yrke)
    }

    private fun inspiserArbeidsavtale(arbeidsavtale: Arbeidsavtale) {
        with (arbeidsavtale) {
            sjekkOmFeltErBlank(this, "yrke", yrke)
            sjekkOmFeltErNull(this, "stillingsprosent", stillingsprosent)
            if (stillingsprosent != null) {
                sjekkProsent(this, "stillingsprosent", stillingsprosent)
            }
            sjekkOmFraOgMedDatoErStørreEnnTilOgMedDato(this, "fom,tom", fom, tom)
        }
    }

    private fun inspiserPermisjon(permisjon: Permisjon) {
        with (permisjon) {
            sjekkProsent(this, "permisjonsprosent", permisjonsprosent)
            sjekkOmFraOgMedDatoErStørreEnnTilOgMedDato(this, "fom,tom", fom, tom)
        }
    }

    fun inntektGjelderEnAnnenAktør(inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt) {
        andreAktørerCounter.inc()
        log.warn("Inntekt gjelder for annen aktør (${(inntekt.inntektsmottaker as AktoerId).aktoerId}) enn det vi forventet")
        sendDatakvalitetEvent(inntekt, "inntektsmottaker", Observasjonstype.InntektGjelderEnAnnenAktør, "Inntekt gjelder for annen aktør")
    }

    fun beløpErMindreEnnNull(objekt: Any, felt: String, verdi: BigDecimal) {
        sendDatakvalitetEvent(objekt, felt, Observasjonstype.ErMindreEnnNull, "$verdi er mindre enn 0")
    }

    fun inntektErUtenforSøkeperiode(inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt) {
        log.info("utbetaltIPeriode (${inntekt.utbetaltIPeriode.toLocalDate()}) er utenfor perioden")
        inntekterUtenforPeriodeCounter.inc()
    }

    fun tellInntektutbetaler(inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt) {
        inntektCounter.labels(when (inntekt) {
            is YtelseFraOffentlige -> "ytelse"
            is PensjonEllerTrygd -> "pensjonEllerTrygd"
            is Naeringsinntekt -> "næring"
            is Loennsinntekt -> "lønn"
            else -> "ukjent"
        }).inc()
    }

    fun tellVirksomhetstypeForInntektutbetaler(inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt) {
        inntektArbeidsgivertypeCounter.labels(when (inntekt.virksomhet) {
            is Organisasjon -> "organisasjon"
            is PersonIdent -> "personIdent"
            is AktoerId -> "aktoerId"
            else -> "ukjent"
        }).inc()
    }

    fun tellArbeidsforholdPerInntekt(arbeidsforholdliste: List<Arbeidsforhold>) {
        arbeidsforholdPerInntektHistogram.observe(arbeidsforholdliste.size.toDouble())
    }

    fun tellAvvikPåArbeidsforhold(arbeidsforholdliste: List<Arbeidsforhold>) {
        if (arbeidsforholdliste.isNotEmpty()) {
            arbeidsforholdliste.forEach { arbeidsforhold ->
                arbeidsforholdAvviksCounter.labels(arbeidsforhold.type()).inc()
                log.info("did not find inntekter for arbeidsforhold (${arbeidsforhold.type()}) with arbeidsgiver=${arbeidsforhold.arbeidsgiver}")
            }
        }
    }

    fun tellAvvikPåInntekter(inntekter: List<Inntekt.Lønn>) {
        if (inntekter.isNotEmpty()) {
            inntektAvviksCounter.inc(inntekter.size.toDouble())
            log.info("did not find arbeidsforhold for ${inntekter.size} inntekter: ${inntekter.joinToString { "${it.type()} - ${it.virksomhet}" }}")
        }
    }

    fun frilansArbeidsforhold(arbeidsforholdliste: List<Arbeidsforhold.Frilans>) {
        frilansCounter.inc(arbeidsforholdliste.size.toDouble())
    }

    private fun flereGjeldendeArbeidsavtaler(objekt: Any, felt: String, verdi: Int) {
        sendDatakvalitetEvent(objekt, felt, Observasjonstype.FlereGjeldendeArbeidsavtaler, "det er $verdi gjeldende arbeidsavtaler")
    }

    private fun sendDatakvalitetEvent(objekt: Any, felt: String, observasjonstype: Observasjonstype, beskrivelse: String) {
        log.info("objekt=${objekt.javaClass.name} felt=$felt feil=$observasjonstype: $beskrivelse")
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

    private fun sjekkOmListeErTom(objekt: Any, felt: String, value: List<Any>) {
        if (value.isEmpty()) {
            sendDatakvalitetEvent(objekt, felt, Observasjonstype.TomListe, "tom liste: $felt er tom")
        }
    }

    private fun sjekkOmFeltErNull(objekt: Any, felt: String, value: Any?) {
        if (value == null) {
            sendDatakvalitetEvent(objekt, felt, Observasjonstype.VerdiMangler, "felt manger: $felt er null")
        }
    }

    private fun sjekkOmFeltErBlank(objekt: Any, felt: String, value: String) {
        if (value.isBlank()) {
            sendDatakvalitetEvent(objekt, felt, Observasjonstype.TomVerdi, "tomt felt: $felt er tom, eller består bare av whitespace")
        }
    }

    private fun sjekkProsent(objekt: Any, felt: String, percent: BigDecimal) {
        if (percent < BigDecimal.ZERO) {
            sendDatakvalitetEvent(objekt, felt, Observasjonstype.ErMindreEnnNull, "ugyldig prosent: $percent % er mindre enn 0 %")
        }

        if (percent > BigDecimal(100)) {
            sendDatakvalitetEvent(objekt, felt, Observasjonstype.ErStørreEnnHundre, "ugyldig prosent: $percent % er større enn 100 %")
        }
    }

    private fun sjekkOmDatoErIFremtiden(objekt: Any, felt: String, dato: LocalDate?) {
        if (dato != null && dato > LocalDate.now()) {
            sendDatakvalitetEvent(objekt, felt, Observasjonstype.DatoErIFremtiden, "ugyldig dato: $dato er i fremtiden")
        }
    }

    private fun sjekkOmFraOgMedDatoErStørreEnnTilOgMedDato(objekt: Any, felt: String, fom: LocalDate, tom: LocalDate?) {
        if (tom != null && fom > tom) {
            sendDatakvalitetEvent(objekt, felt, Observasjonstype.FraOgMedDatoErStørreEnnTilOgMedDato, "ugyldig dato: $fom er større enn $tom")
        }
    }

    private fun sjekkOmStartdatoErStørreEnnSluttdato(objekt: Any, felt: String, startdato: LocalDate, sluttdato: LocalDate?) {
        if (sluttdato != null && startdato > sluttdato) {
            sendDatakvalitetEvent(objekt, felt, Observasjonstype.StartdatoStørreEnnSluttdato, "ugyldig dato: $startdato er større enn $sluttdato")
        }
    }
}
