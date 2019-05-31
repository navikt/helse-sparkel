package no.nav.helse.probe

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.aiy.domain.*
import no.nav.helse.domene.aiy.organisasjon.OrganisasjonService
import no.nav.helse.domene.ytelse.domain.*
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.InfotrygdSak
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.InfotrygdVedtak
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate

class DatakvalitetProbe(sensuClient: SensuClient, private val organisasjonService: OrganisasjonService) {

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
        VerdiManglerIkke,
        TomVerdi,
        HarVerdi,
        FlereArbeidsforholdPerInntekt,
        ArbeidsforholdISammeVirksomhet,
        IngenArbeidsforholdForInntekt,
        ErMindreEnnNull,
        InntektGjelderEnAnnenAktør,
        UlikeYrkerForArbeidsforhold,
        UlikeArbeidsforholdMedSammeYrke,
        UlikeArbeidsforholdMedUlikYrke,
        VirksomhetErNavAktør,
        VirksomhetErPerson,
        VirksomhetErOrganisasjon,
        OrganisasjonErJuridiskEnhet,
        OrganisasjonErVirksomhet,
        OrganisasjonErOrganisasjonsledd,
        UkjentTema,
        UtbetalingsgradMangler,
        HullIAnvistPeriode,
        AnvistPeriodeOk,
        FinnerIkkeGrunnlag,
        FinnerGrunnlag
    }

    fun inspiserArbeidstaker(arbeidsforhold: Arbeidsforhold.Arbeidstaker) {
        sjekkOmStartdatoErStørreEnnSluttdato(arbeidsforhold, "startdato,sluttdato", arbeidsforhold.startdato, arbeidsforhold.sluttdato)
        sjekkOmDatoErIFremtiden(arbeidsforhold, "sluttdato", arbeidsforhold.sluttdato)
        sjekkArbeidsgiver(arbeidsforhold)

        arbeidsforhold.permisjon.forEach { permisjon ->
            inspiserPermisjon(permisjon)
        }

        arbeidsforhold.arbeidsavtaler.forEach { arbeidsavtale ->
            inspiserArbeidsavtale(arbeidsavtale)
        }

        val ulikeYrkerForArbeidsforhold = arbeidsforhold.arbeidsavtaler.distinctBy { arbeidsavtale ->
            arbeidsavtale.yrke
        }.size

        if (ulikeYrkerForArbeidsforhold > 1) {
            sendDatakvalitetEvent(arbeidsforhold, "arbeidsavtale", Observasjonstype.UlikeYrkerForArbeidsforhold, "arbeidsforhold har $ulikeYrkerForArbeidsforhold forskjellige yrkeskoder")
        }
    }

    fun inspiserUtbetalingEllerTrekk(utbetalingEllerTrekk: UtbetalingEllerTrekk) {
        if (utbetalingEllerTrekk.beløp < BigDecimal.ZERO) {
            beløpErMindreEnnNull(utbetalingEllerTrekk, "beløp", utbetalingEllerTrekk.beløp)
        }

        when (utbetalingEllerTrekk.virksomhet) {
            is Virksomhet.NavAktør -> sendDatakvalitetEvent(utbetalingEllerTrekk, "virksomhet", Observasjonstype.VirksomhetErNavAktør, "utbetaling er gjort av en NavAktør")
            is Virksomhet.Person -> sendDatakvalitetEvent(utbetalingEllerTrekk, "virksomhet", Observasjonstype.VirksomhetErPerson, "utbetaling er gjort av en person")
            is Virksomhet.Organisasjon -> {
                sendDatakvalitetEvent(utbetalingEllerTrekk, "virksomhet", Observasjonstype.VirksomhetErOrganisasjon, "utbetaling er gjort av en organisasjon")

                organisasjonService.hentOrganisasjon((utbetalingEllerTrekk.virksomhet as Virksomhet.Organisasjon).organisasjonsnummer).fold({ feil ->
                    log.info("feil ved henting av organisasjon ${(utbetalingEllerTrekk.virksomhet as Virksomhet.Organisasjon).organisasjonsnummer}: $feil")
                }, { organisasjon ->
                    when (organisasjon) {
                        is no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.JuridiskEnhet -> sendDatakvalitetEvent(utbetalingEllerTrekk, "virksomhet", Observasjonstype.OrganisasjonErJuridiskEnhet, "utbetaling er gjort av en juridisk enhet")
                        is no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Virksomhet -> sendDatakvalitetEvent(utbetalingEllerTrekk, "virksomhet", Observasjonstype.OrganisasjonErVirksomhet, "utbetaling er gjort av en virksomhet")
                        is no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Organisasjonsledd -> sendDatakvalitetEvent(utbetalingEllerTrekk, "virksomhet", Observasjonstype.OrganisasjonErOrganisasjonsledd, "utbetaling er gjort av et organisasjonsledd")
                    }
                })
            }
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

            arbeidInntektYtelse.arbeidsforhold.groupBy {
                it.arbeidsgiver
            }.filter {
                it.value.size > 1
            }.forEach {
                val arbeidsforholdEtterYrke = it.value.filter {
                    it is Arbeidsforhold.Arbeidstaker
                }.groupBy {
                    (it as Arbeidsforhold.Arbeidstaker).yrke()
                }

                if (arbeidsforholdEtterYrke.size > 1) {
                    sendDatakvalitetEvent(arbeidInntektYtelse, "arbeidsforhold", Observasjonstype.UlikeArbeidsforholdMedUlikYrke, "søker har ${arbeidsforholdEtterYrke.size} ulike yrker i samme virksomhet")
                }

                val ulikeArbeidsforholdMedSammeYrke = arbeidsforholdEtterYrke.filter {
                    it.value.size > 1
                }.flatMap {
                    it.value
                }.size

                if (ulikeArbeidsforholdMedSammeYrke > 0) {
                    sendDatakvalitetEvent(arbeidInntektYtelse, "arbeidsforhold", Observasjonstype.UlikeArbeidsforholdMedSammeYrke, "søker har $ulikeArbeidsforholdMedSammeYrke ulike arbeidsforhold i samme virksomhet, med samme yrkeskode")
                }
            }
        }

        tellAvvikPåInntekter(arbeidInntektYtelse)
        tellAvvikPåArbeidsforhold(arbeidInntektYtelse)
    }

    fun inspiserFrilans(arbeidsforhold: Arbeidsforhold.Frilans) {
        sjekkOmStartdatoErStørreEnnSluttdato(arbeidsforhold, "startdato,sluttdato", arbeidsforhold.startdato, arbeidsforhold.sluttdato)
        sjekkOmDatoErIFremtiden(arbeidsforhold, "sluttdato", arbeidsforhold.sluttdato)
        sjekkOmFeltErBlank(arbeidsforhold, "yrke", arbeidsforhold.yrke)
        sjekkArbeidsgiver(arbeidsforhold)
    }

    fun inspiserInfotrygdSak(sak: InfotrygdSak) {
        sjekkOmFeltErNull(sak, "sakId", sak.sakId)
        sak.sakId?.let {
            sjekkOmFeltErBlank(sak, "sakId", it)
        }

        sjekkOmFeltErNull(sak, "registrert", sak.registrert)

        sjekkOmFeltErNull(sak, "type", sak.type?.value)
        sak.type?.value?.let {
            sjekkOmFeltErBlank(sak, "type", it)
        }

        sjekkOmFeltErNull(sak, "status", sak.status?.value)
        sak.status?.value?.let {
            sjekkOmFeltErBlank(sak, "status", it)
        }

        sjekkOmFeltErNull(sak, "resultat", sak.resultat?.value)
        sak.resultat?.value?.let {
            sjekkOmFeltErBlank(sak, "resultat", it)
        }

        sjekkOmFeltErNull(sak, "iverksatt", sak.iverksatt)
        sjekkOmFeltErNull(sak, "vedtatt", sak.vedtatt)

        if (sak is InfotrygdVedtak) {
            sjekkOmFeltErNull(sak, "opphoerFom", sak.opphoerFom)
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

    private fun sjekkArbeidsgiver(arbeidsforhold: Arbeidsforhold) {
        when (arbeidsforhold.arbeidsgiver) {
            is Virksomhet.NavAktør -> sendDatakvalitetEvent(arbeidsforhold, "arbeidsgiver", Observasjonstype.VirksomhetErNavAktør, "arbeidsgiver er en NavAktør")
            is Virksomhet.Person -> sendDatakvalitetEvent(arbeidsforhold, "arbeidsgiver", Observasjonstype.VirksomhetErPerson, "arbeidsgiver er en person")
            is Virksomhet.Organisasjon -> {
                sendDatakvalitetEvent(arbeidsforhold, "arbeidsgiver", Observasjonstype.VirksomhetErOrganisasjon, "arbeidsgiver er en organisasjon")

                organisasjonService.hentOrganisasjon((arbeidsforhold.arbeidsgiver as Virksomhet.Organisasjon).organisasjonsnummer).fold({ feil ->
                    log.info("feil ved henting av organisasjon ${(arbeidsforhold.arbeidsgiver as Virksomhet.Organisasjon).organisasjonsnummer}: $feil")
                }, { organisasjon ->
                    when (organisasjon) {
                        is no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.JuridiskEnhet -> sendDatakvalitetEvent(arbeidsforhold, "arbeidsgiver", Observasjonstype.OrganisasjonErJuridiskEnhet, "arbeidsgiver er en juridisk enhet")
                        is no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Virksomhet -> sendDatakvalitetEvent(arbeidsforhold, "arbeidsgiver", Observasjonstype.OrganisasjonErVirksomhet, "arbeidsgiver er en virksomhet")
                        is no.nav.helse.domene.aiy.organisasjon.domain.Organisasjon.Organisasjonsledd -> sendDatakvalitetEvent(arbeidsforhold, "arbeidsgiver", Observasjonstype.OrganisasjonErOrganisasjonsledd, "arbeidsgiver er et organisasjonsledd")
                    }
                })
            }
        }
    }

    private fun inspiserArbeidsavtale(arbeidsavtale: Arbeidsavtale) {
        with (arbeidsavtale) {
            sjekkOmFeltErBlank(this, "yrke", yrke)
            sjekkOmFeltErNull(this, "stillingsprosent", stillingsprosent)
            if (stillingsprosent != null) {
                sjekkProsent(this, "stillingsprosent", stillingsprosent)
            }

            if (this is Arbeidsavtale.Historisk) {
                sjekkOmFraOgMedDatoErStørreEnnTilOgMedDato(this, "fom,tom", fom, tom)
            }
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

    private fun tellAvvikPåArbeidsforhold(arbeidInntektYtelse: ArbeidInntektYtelse) {
        arbeidInntektYtelse.arbeidsforhold.filterNot { arbeidsforhold ->
            arbeidInntektYtelse.lønnsinntekter.any { (_, muligeArbeidsforhold) ->
                muligeArbeidsforhold.any { it == arbeidsforhold }
            }
        }.let { arbeidsforholdUtenInntekter ->
            arbeidsforholdUtenInntekter.forEach { arbeidsforhold ->
                arbeidsforholdAvviksCounter.labels(arbeidsforhold.type()).inc()
                log.info("did not find inntekter for arbeidsforhold (${arbeidsforhold.type()}) with arbeidsgiver=${arbeidsforhold.arbeidsgiver}")
            }
        }
    }

    private fun tellAvvikPåInntekter(arbeidInntektYtelse: ArbeidInntektYtelse) {
        arbeidInntektYtelse.lønnsinntekter.forEach { (inntekt, muligeArbeidsforhold) ->
            arbeidsforholdPerInntektHistogram.observe(muligeArbeidsforhold.size.toDouble())

            if (muligeArbeidsforhold.size > 1) {
                sendDatakvalitetEvent(arbeidInntektYtelse, "lønnsinntekter", Observasjonstype.FlereArbeidsforholdPerInntekt, "det er ${muligeArbeidsforhold.size} potensielle arbeidsforhold for inntekt")
            } else if (muligeArbeidsforhold.isEmpty()) {
                sendDatakvalitetEvent(arbeidInntektYtelse, "lønnsinntekter", Observasjonstype.IngenArbeidsforholdForInntekt, "det er ingen potensielle arbeidsforhold for inntekt")
                inntektAvviksCounter.inc()
                log.info("did not find arbeidsforhold for inntekt: ${inntekt.type()} - ${inntekt.virksomhet}")
            }
        }
    }

    fun frilansArbeidsforhold(arbeidsforholdliste: List<Arbeidsforhold.Frilans>) {
        frilansCounter.inc(arbeidsforholdliste.size.toDouble())
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
