package no.nav.helse.probe

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.helse.common.toLocalDate
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
        ProsentMindreEnnNull,
        ProsentStørreEnnHundre,
        DatoStørreEnn,
        NullVerdi,
        TomVerdi,
        TomListe,
        FlereGjeldendeArbeidsavtaler,
        ArbeidsforholdISammeVirksomhet,
        BeløpErMindreEnnNull,
        InntektGjelderEnAnnenAktør
    }

    fun inspiserArbeidstaker(arbeidsforhold: Arbeidsforhold.Arbeidstaker) {
        sjekkOmDatoErStørreEnn("Arbeid", arbeidsforhold, "startdato,sluttdato", arbeidsforhold.startdato, arbeidsforhold.sluttdato)

        arbeidsforhold.permisjon.forEach { permisjon ->
            inspiserPermisjon(permisjon)
        }

        sjekkOmListeErTom("Arbeid", arbeidsforhold, "arbeidsavtaler", arbeidsforhold.arbeidsavtaler)

        val gjeldendeArbeidsavtaler = arbeidsforhold.arbeidsavtaler.filter { it.tom == null }.size
        if (gjeldendeArbeidsavtaler > 1) {
            flereGjeldendeArbeidsavtaler("Arbeid", arbeidsforhold, "arbeidsavtaler", gjeldendeArbeidsavtaler)
        }

        arbeidsforhold.arbeidsavtaler.forEach { arbeidsavtale ->
            inspiserArbeidsavtale(arbeidsavtale)
        }
    }

    fun inspiserInntekt(inntekt: Inntekt) {
        if (inntekt.beløp < BigDecimal.ZERO) {
            beløpErMindreEnnNull("Inntekt", inntekt, "beløp", inntekt.beløp)
        }
    }

    fun inspiserArbeidsforholdISammeVirksomhet(arbeidsforholdliste: List<Arbeidsforhold>) {
        arbeidsforholdHistogram.observe(arbeidsforholdliste.size.toDouble())

        val unikeArbeidsgivere = arbeidsforholdliste.distinctBy {
            it.arbeidsgiver
        }
        val arbeidsforholdISammeVirksomhet = arbeidsforholdliste.size - unikeArbeidsgivere.size

        if (arbeidsforholdISammeVirksomhet > 0) {
            arbeidsforholdISammeVirksomhet("Arbeid", arbeidsforholdliste, arbeidsforholdISammeVirksomhet)
        }
    }

    fun inspiserFrilans(arbeidsforhold: Arbeidsforhold.Frilans) {
        sjekkOmDatoErStørreEnn("Arbeid", arbeidsforhold, "startdato,sluttdato", arbeidsforhold.startdato, arbeidsforhold.sluttdato)
        sjekkOmFeltErBlank("Arbeid", arbeidsforhold, "yrke", arbeidsforhold.yrke)
    }

    private fun inspiserArbeidsavtale(arbeidsavtale: Arbeidsavtale) {
        with (arbeidsavtale) {
            sjekkOmFeltErBlank("Arbeid", this, "yrke", yrke)
            sjekkOmFeltErNull("Arbeid", this, "stillingsprosent", stillingsprosent)
            if (stillingsprosent != null) {
                sjekkProsent("Arbeid", this, "stillingsprosent", stillingsprosent)
            }
            sjekkOmDatoErStørreEnn("Arbeid", this, "fom,tom", fom, tom)
        }
    }

    private fun inspiserPermisjon(permisjon: Permisjon) {
        with (permisjon) {
            sjekkProsent("Arbeid", this, "permisjonsprosent", permisjonsprosent)
            sjekkOmDatoErStørreEnn("Arbeid", this, "fom,tom", fom, tom)
        }
    }

    fun inntektGjelderEnAnnenAktør(inntekt: no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Inntekt) {
        andreAktørerCounter.inc()
        log.warn("Inntekt gjelder for annen aktør (${(inntekt.inntektsmottaker as AktoerId).aktoerId}) enn det vi forventet")
        sendDatakvalitetEvent("Inntekt", inntekt, "inntektsmottaker", Observasjonstype.InntektGjelderEnAnnenAktør, "Inntekt gjelder for annen aktør")
    }

    fun beløpErMindreEnnNull(domain: String, objekt: Any, felt: String, verdi: BigDecimal) {
        sendDatakvalitetEvent(domain, objekt, felt, Observasjonstype.BeløpErMindreEnnNull, "$verdi er mindre enn 0")
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

    private fun arbeidsforholdISammeVirksomhet(domain: String, objekt: Any, antall: Int) {
        arbeidsforholdISammeVirksomhetCounter.inc(antall.toDouble())
        sendDatakvalitetEvent(domain, objekt, null, Observasjonstype.ArbeidsforholdISammeVirksomhet, "fant $antall arbeidsforhold i samme virksomhet")
    }

    private fun flereGjeldendeArbeidsavtaler(domain: String, objekt: Any, felt: String, verdi: Int) {
        sendDatakvalitetEvent(domain, objekt, felt, Observasjonstype.FlereGjeldendeArbeidsavtaler, "det er $verdi gjeldende arbeidsavtaler")
    }

    private fun nullVerdi(domain: String, objekt: Any, felt: String) {
        sendDatakvalitetEvent(domain, objekt, felt, Observasjonstype.NullVerdi, "felt manger: $felt er null")
    }

    private fun tomListe(domain: String, objekt: Any, felt: String) {
        sendDatakvalitetEvent(domain, objekt, felt, Observasjonstype.TomListe, "tom liste: $felt er tom")
    }

    private fun tomVerdi(domain: String, objekt: Any, felt: String) {
        sendDatakvalitetEvent(domain, objekt, felt, Observasjonstype.TomVerdi, "tomt felt: $felt er tom, eller består bare av whitespace")
    }

    private fun prosentMindreEnnNull(domain: String, objekt: Any, felt: String, prosent: BigDecimal) {
        sendDatakvalitetEvent(domain, objekt, felt, Observasjonstype.ProsentMindreEnnNull, "ugyldig prosent: $prosent % er mindre enn 0 %")
    }

    private fun prosentStørreEnnHundre(domain: String, objekt: Any, felt: String, prosent: BigDecimal) {
        sendDatakvalitetEvent(domain, objekt, felt, Observasjonstype.ProsentStørreEnnHundre, "ugyldig prosent: $prosent % er større enn 100 %")
    }

    private fun datoErStørreEnn(domain: String, objekt: Any, felt: String, dato1: LocalDate, dato2: LocalDate) {
        sendDatakvalitetEvent(domain, objekt, felt, Observasjonstype.DatoStørreEnn, "ugyldig dato: $dato1 er større enn $dato2")
    }

    private fun sendDatakvalitetEvent(domain: String, objekt: Any, felt: String?, observasjonstype: Observasjonstype, beskrivelse: String) {
        log.info("domain=$domain objekt=${objekt.javaClass.name} ${if (felt != null) "felt=$felt " else "" }feil=$observasjonstype: $beskrivelse")
        influxMetricReporter.sendDataPoint("datakvalitet.event",
                mapOf(
                        "beskrivelse" to beskrivelse
                ),
                mapOf(
                        "domain" to domain,
                        "objekt" to objekt.javaClass.name,
                        "type" to observasjonstype.name
                ))
    }

    private fun sjekkOmListeErTom(domain: String, objekt: Any, felt: String, value: List<Any>) {
        if (value.isEmpty()) {
            tomListe(domain, objekt, felt)
        }
    }

    private fun sjekkOmFeltErNull(domain: String, objekt: Any, felt: String, value: Any?) {
        if (value == null) {
            nullVerdi(domain, objekt, felt)
        }
    }

    private fun sjekkOmFeltErBlank(domain: String, objekt: Any, felt: String, value: String) {
        if (value.isBlank()) {
            tomVerdi(domain, objekt, felt)
        }
    }

    private fun sjekkProsent(domain: String, objekt: Any, felt: String, percent: BigDecimal) {
        if (percent < BigDecimal.ZERO) {
            prosentMindreEnnNull(domain, objekt, felt, percent)
        }

        if (percent > BigDecimal(100)) {
            prosentStørreEnnHundre(domain, objekt, felt, percent)
        }
    }

    private fun sjekkOmDatoErStørreEnn(domain: String, objekt: Any, felt: String, dato1: LocalDate, dato2: LocalDate?) {
        if (dato2 != null && dato1 > dato2) {
            datoErStørreEnn(domain, objekt, felt, dato1, dato2)
        }
    }
}
