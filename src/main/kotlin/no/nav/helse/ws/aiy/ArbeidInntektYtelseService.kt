package no.nav.helse.ws.aiy

import arrow.core.Either
import arrow.core.flatMap
import io.prometheus.client.Counter
import no.nav.helse.Feilårsak
import no.nav.helse.arrow.sequenceU
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdService
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold
import no.nav.helse.ws.inntekt.InntektService
import no.nav.helse.ws.inntekt.domain.Inntekt
import no.nav.helse.ws.inntekt.domain.Virksomhet
import no.nav.helse.ws.organisasjon.OrganisasjonService
import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class ArbeidInntektYtelseService(private val arbeidsforholdService: ArbeidsforholdService, private val inntektService: InntektService, private val organisasjonService: OrganisasjonService) {

    companion object {
        private val log = LoggerFactory.getLogger(ArbeidInntektYtelseService::class.java)

        private val inntektfilter = "ForeldrepengerA-Inntekt"

        private val arbeidsforholdAvviksCounter = Counter.build()
                .name("arbeidsforhold_avvik_totals")
                .labelNames("type")
                .help("antall arbeidsforhold som ikke har noen tilhørende inntekter")
                .register()

        private val foreløpigArbeidsforholdAvviksCounter = Counter.build()
                .name("forelopig_arbeidsforhold_avvik_totals")
                .labelNames("type")
                .help("antall arbeidsforhold som ikke har noen tilhørende inntekter, før vi slår opp virksomhetsnummer")
                .register()
        private val inntektAvviksCounter = Counter.build()
                .name("inntekt_avvik_totals")
                .help("antall inntekter som ikke har noen tilhørende arbeidsforhold")
                .register()

        private val foreløpigInntektAvviksCounter = Counter.build()
                .name("forelopig_inntekt_avvik_totals")
                .help("antall inntekter som ikke har noen tilhørende arbeidsforhold, før vi slår opp virksomhetsnummer")
                .register()

        private val virksomhetsCounter = Counter.build()
                .name("inntekt_virksomheter_totals")
                .labelNames("type")
                .help("antall inntekter fordelt på ulike virksomhetstyper (juridisk enhet eller virksomhet)")
                .register()

        private val juridiskTilVirksomhetsnummerCounter = Counter.build()
                .name("juridisk_til_virksomhetsnummer_totals")
                .help("antall ganger vi har funnet virksomhetsnummer fra juridisk nummer")
                .register()
    }

    fun finnArbeidInntekterOgYtelser(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            finnInntekterOgFordelEtterType(aktørId, YearMonth.from(fom), YearMonth.from(tom)) { lønnsinntekter, ytelser, pensjonEllerTrygd, næringsinntekter ->
                arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom).flatMap { kombinertArbeidsforholdliste ->
                    kombinerArbeidsforholdOgInntekt(lønnsinntekter, kombinertArbeidsforholdliste).map { (inntekterEtterArbeidsforhold, arbeidsforholdUtenInntekter, inntekterUtenArbeidsforhold) ->
                        ArbeidInntektYtelse(inntekterEtterArbeidsforhold, inntekterUtenArbeidsforhold, arbeidsforholdUtenInntekter, ytelser, pensjonEllerTrygd, næringsinntekter)
                    }
                }
            }

    private fun <R> finnInntekterOgFordelEtterType(aktørId: AktørId, fom: YearMonth, tom: YearMonth, callback: ArbeidInntektYtelseService.(List<Inntekt.Lønn>, List<Inntekt.Ytelse>, List<Inntekt.PensjonEllerTrygd>, List<Inntekt.Næring>) -> Either<Feilårsak, R>) =
            inntektService.hentInntekter(aktørId, fom, tom, inntektfilter).flatMap { inntekter ->
                val lønnsinntekter = mutableListOf<Inntekt.Lønn>()
                val ytelser = mutableListOf<Inntekt.Ytelse>()
                val pensjonEllerTrygd = mutableListOf<Inntekt.PensjonEllerTrygd>()
                val næringsinntekter = mutableListOf<Inntekt.Næring>()

                inntekter.forEach { inntekt ->
                    when (inntekt) {
                        is Inntekt.Lønn -> lønnsinntekter.add(inntekt)
                        is Inntekt.Ytelse -> ytelser.add(inntekt)
                        is Inntekt.PensjonEllerTrygd -> pensjonEllerTrygd.add(inntekt)
                        is Inntekt.Næring -> næringsinntekter.add(inntekt)
                    }
                }

                this.callback(lønnsinntekter, ytelser, pensjonEllerTrygd, næringsinntekter)
            }

    private fun kombinerArbeidsforholdOgInntekt(lønnsinntekter: List<Inntekt.Lønn>, arbeidsforholdliste: List<Arbeidsforhold>) =
            splittInntekterMedOgUtenArbeidsforhold(lønnsinntekter, arbeidsforholdliste) { foreløpigInntekterMedArbeidsforhold, foreløpigArbeidsforholdUtenInntekt, foreløpigInntekterUtenArbeidsforhold ->
                tellForeløpigAvvikPåArbeidsforhold(foreløpigArbeidsforholdUtenInntekt)
                tellForeløpigAvvikPåInntekter(foreløpigInntekterUtenArbeidsforhold)

                hentVirksomhetsnummerForInntekterRegistrertPåJuridiskNummer(foreløpigInntekterUtenArbeidsforhold)
                        .map { inntekterUtenArbeidsforholdMedOppdatertVirksomhetsnummer ->
                            splittInntekterMedOgUtenArbeidsforhold(foreløpigInntekterMedArbeidsforhold, inntekterUtenArbeidsforholdMedOppdatertVirksomhetsnummer, arbeidsforholdliste) { inntekterMedArbeidsforhold, arbeidsforholdUtenInntekt, inntekterUtenArbeidsforhold ->
                                tellAvvikPåArbeidsforhold(arbeidsforholdUtenInntekt)
                                tellAvvikPåInntekter(inntekterUtenArbeidsforhold)

                                Triple(
                                        first = grupperInntekterEtterArbeidsforholdOgPeriode(inntekterMedArbeidsforhold),
                                        second = arbeidsforholdUtenInntekt,
                                        third = inntekterUtenArbeidsforhold
                                )
                            }
                        }
            }

    private fun <R> splittInntekterMedOgUtenArbeidsforhold(lønnsinntekter: List<Inntekt.Lønn>, arbeidsforholdliste: List<Arbeidsforhold>, callback: ArbeidInntektYtelseService.(Map<Arbeidsforhold, List<Inntekt.Lønn>>, List<Arbeidsforhold>, List<Inntekt.Lønn>) -> R) =
            arbeidsforholdliste.combineAndComputeDiff(lønnsinntekter) { arbeidsforhold, inntekt ->
                arbeidsforhold.arbeidsgiver == inntekt.virksomhet
            }.let { (arbeidsforholdMedInntekter, arbeidsforholdUtenInntekter, inntekterUtenArbeidsforhold) ->
                callback(this, arbeidsforholdMedInntekter, arbeidsforholdUtenInntekter, inntekterUtenArbeidsforhold)
            }

    private fun <R> splittInntekterMedOgUtenArbeidsforhold(inntekterMedArbeidsforhold: Map<Arbeidsforhold, List<Inntekt.Lønn>>, lønnsinntekter: List<Inntekt.Lønn>, arbeidsforholdliste: List<Arbeidsforhold>, callback: ArbeidInntektYtelseService.(Map<Arbeidsforhold, List<Inntekt.Lønn>>, List<Arbeidsforhold>, List<Inntekt.Lønn>) -> R) =
            splittInntekterMedOgUtenArbeidsforhold(inntekterMedArbeidsforhold.flatMap { it.value } + lønnsinntekter, arbeidsforholdliste, callback)

    private fun hentVirksomhetsnummerForInntekterRegistrertPåJuridiskNummer(inntekter: List<Inntekt.Lønn>) =
            inntekter.map { inntekt ->
                hentVirksomhetsnummer(inntekt)
            }.sequenceU()

    private fun hentVirksomhetsnummer(inntekt: Inntekt.Lønn) =
            when (inntekt.virksomhet) {
                is Virksomhet.Organisasjon -> organisasjonService.hentOrganisasjon(Organisasjonsnummer(inntekt.virksomhet.identifikator)).flatMap { organisasjon ->
                    virksomhetsCounter.labels(organisasjon.type()).inc()

                    when (organisasjon) {
                        is no.nav.helse.ws.organisasjon.domain.Organisasjon.JuridiskEnhet -> organisasjonService.hentVirksomhetForJuridiskOrganisasjonsnummer(organisasjon.orgnr, inntekt.utbetalingsperiode.atDay(1)).fold({
                            log.warn("error while looking up virksomhetsnummer for juridisk enhet, responding with the juridisk organisasjonsnummer (${organisasjon.orgnr}) instead")
                            Either.Right(inntekt)
                        }, { virksomhetsnummer ->
                            log.info("slo opp virksomhetsnummer for ${organisasjon.orgnr}")
                            juridiskTilVirksomhetsnummerCounter.inc()
                            Either.Right(inntekt.copy(
                                    virksomhet = Virksomhet.Organisasjon(virksomhetsnummer)
                            ))
                        })
                        else -> Either.Right(inntekt)
                    }
                }
                else -> Either.Right(inntekt)
            }

    private fun grupperInntekterEtterArbeidsforholdOgPeriode(inntekterMedArbeidsforhold: Map<Arbeidsforhold, List<Inntekt.Lønn>>) =
            inntekterMedArbeidsforhold.mapValues { entry ->
                entry.value.groupBy { inntekt ->
                    inntekt.utbetalingsperiode
                }
            }

    private fun tellForeløpigAvvikPåArbeidsforhold(arbeidsforholdliste: List<Arbeidsforhold>) {
        if (arbeidsforholdliste.isNotEmpty()) {
            log.warn("fant foreløpig ${arbeidsforholdliste.size} arbeidsforhold hvor vi ikke finner inntekter: ${arbeidsforholdliste.joinToString { "${it.type()} - ${it.arbeidsgiver}" }}")
            arbeidsforholdliste.forEach { arbeidsforhold ->
                foreløpigArbeidsforholdAvviksCounter.labels(arbeidsforhold.type()).inc()
            }
        }
    }

    private fun tellForeløpigAvvikPåInntekter(inntekter: List<Inntekt.Lønn>) {
        if (inntekter.isNotEmpty()) {
            log.warn("fant foreløpig ${inntekter.size} inntekter hvor vi ikke finner arbeidsforhold: ${inntekter.joinToString { "${it.type()} - ${it.virksomhet}" }}")
            foreløpigInntektAvviksCounter.inc(inntekter.size.toDouble())
        }
    }

    private fun tellAvvikPåArbeidsforhold(arbeidsforholdliste: List<Arbeidsforhold>) {
        if (arbeidsforholdliste.isNotEmpty()) {
            arbeidsforholdliste.forEach { arbeidsforhold ->
                arbeidsforholdAvviksCounter.labels(arbeidsforhold.type()).inc()
                log.warn("did not find inntekter for arbeidsforhold (${arbeidsforhold.type()}) with arbeidsgiver=${arbeidsforhold.arbeidsgiver}")
            }
        }
    }

    private fun tellAvvikPåInntekter(inntekter: List<Inntekt.Lønn>) {
        if (inntekter.isNotEmpty()) {
            inntektAvviksCounter.inc(inntekter.size.toDouble())
            log.warn("did not find arbeidsforhold for ${inntekter.size} inntekter: ${inntekter.joinToString { "${it.type()} - ${it.virksomhet}" }}")
        }
    }
}
