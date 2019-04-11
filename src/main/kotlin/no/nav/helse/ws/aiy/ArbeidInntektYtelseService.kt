package no.nav.helse.ws.aiy

import io.prometheus.client.Counter
import no.nav.helse.*
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.ws.aiy.domain.Arbeidsforhold
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdService
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsgiver
import no.nav.helse.ws.inntekt.InntektService
import no.nav.helse.ws.inntekt.domain.ArbeidsforholdFrilanser
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

        private val arbeidsforholdAvviksCounter = Counter.build()
                .name("arbeidsforhold_avvik_totals")
                .labelNames("type")
                .help("antall arbeidsforhold som ikke har noen tilhørende inntekter")
                .register()
        private val inntektAvviksCounter = Counter.build()
                .name("inntekt_avvik_totals")
                .help("antall inntekter som ikke har noen tilhørende arbeidsforhold")
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
                finnOgKombinerArbeidsforholdOgFrilansArbeidsforhold(aktørId, fom, tom).flatMap { kombinertArbeidsforholdliste ->
                    kombinerArbeidsforholdOgInntekt(lønnsinntekter, kombinertArbeidsforholdliste).map { inntekterEtterArbeidsforhold ->
                        ArbeidInntektYtelse(inntekterEtterArbeidsforhold, ytelser, pensjonEllerTrygd, næringsinntekter)
                    }
                }
            }

    private fun <R> finnInntekterOgFordelEtterType(aktørId: AktørId, fom: YearMonth, tom: YearMonth, callback: ArbeidInntektYtelseService.(List<Inntekt.Lønn>, List<Inntekt.Ytelse>, List<Inntekt.PensjonEllerTrygd>, List<Inntekt.Næring>) -> Either<Feilårsak, R>) =
            inntektService.hentInntekter(aktørId, fom, tom).flatMap { inntekter ->
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

    private fun finnOgKombinerArbeidsforholdOgFrilansArbeidsforhold(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            inntektService.hentFrilansarbeidsforhold(aktørId, YearMonth.from(fom), YearMonth.from(tom)).flatMap { frilansArbeidsforholdliste ->
                arbeidsforholdService.finnArbeidsforhold(aktørId, fom, tom).map { arbeidsforholdliste ->
                    kombinerArbeidsforhold(arbeidsforholdliste, frilansArbeidsforholdliste)
                }
            }

    private fun kombinerArbeidsforhold(arbeidstakerArbeidsforhold: List<no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold>, frilansArbeidsforhold: List<ArbeidsforholdFrilanser>) =
            arbeidstakerArbeidsforhold.map(::tilArbeidsforhold)
                    .plus(frilansArbeidsforhold.map(::tilArbeidsforhold))

    private fun tilArbeidsforhold(arbeidsforhold: no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold) =
            Arbeidsforhold.Arbeidstaker(
                    arbeidsgiver = when (arbeidsforhold.arbeidsgiver) {
                        is Arbeidsgiver.Virksomhet -> Virksomhet.Organisasjon(arbeidsforhold.arbeidsgiver.virksomhet.orgnr)
                        is Arbeidsgiver.Person -> Virksomhet.Person(arbeidsforhold.arbeidsgiver.personnummer)
                    },
                    startdato = arbeidsforhold.startdato,
                    sluttdato = arbeidsforhold.sluttdato
            )

    private fun tilArbeidsforhold(arbeidsforhold: ArbeidsforholdFrilanser) =
            Arbeidsforhold.Frilans(
                    arbeidsgiver = arbeidsforhold.arbeidsgiver,
                    startdato = arbeidsforhold.startdato,
                    sluttdato = arbeidsforhold.sluttdato,
                    yrke = arbeidsforhold.yrke
            )

    private fun kombinerArbeidsforholdOgInntekt(lønnsinntekter: List<Inntekt.Lønn>, arbeidsforholdliste: List<Arbeidsforhold>) =
            splittInntekterMedOgUtenArbeidsforhold(lønnsinntekter, arbeidsforholdliste) { foreløpigArbeidsforholdUtenInntekt, foreløpigInntekterMedArbeidsforhold, foreløpigInntekterUtenArbeidsforhold ->
                if (foreløpigArbeidsforholdUtenInntekt.isNotEmpty()) {
                    log.warn("fant foreløpig ${foreløpigArbeidsforholdUtenInntekt.size} arbeidsforhold hvor vi ikke finner inntekter: ${foreløpigArbeidsforholdUtenInntekt.joinToString { "${it.type()} - ${it.arbeidsgiver}" }}")
                }
                if (foreløpigInntekterUtenArbeidsforhold.isNotEmpty()) {
                    log.warn("fant foreløpig ${foreløpigInntekterUtenArbeidsforhold.size} inntekter hvor vi ikke finner arbeidsforhold: ${foreløpigInntekterUtenArbeidsforhold.joinToString { "${it.type()} - ${it.virksomhet}" }}")
                }

                hentVirksomhetsnummerForInntekterRegistrertPåJuridiskNummer(foreløpigInntekterUtenArbeidsforhold)
                        .map { inntekterUtenArbeidsforholdMedOppdatertVirksomhetsnummer ->
                            splittInntekterMedOgUtenArbeidsforhold(foreløpigInntekterMedArbeidsforhold + inntekterUtenArbeidsforholdMedOppdatertVirksomhetsnummer, arbeidsforholdliste) { arbeidsforholdUtenInntekt, inntekterMedArbeidsforhold, inntekterUtenArbeidsforhold ->
                                if (arbeidsforholdUtenInntekt.isNotEmpty()) {
                                    tellAvvikPåArbeidsforhold(arbeidsforholdUtenInntekt)
                                }
                                if (inntekterUtenArbeidsforhold.isNotEmpty()) {
                                    tellAvvikPåInntekter(inntekterUtenArbeidsforhold)
                                }

                                grupperInntekterEtterArbeidsforholdOgPeriode(inntekterMedArbeidsforhold, arbeidsforholdliste)
                            }
                        }
            }

    private fun <R> splittInntekterMedOgUtenArbeidsforhold(lønnsinntekter: List<Inntekt.Lønn>, arbeidsforholdliste: List<Arbeidsforhold>, callback: ArbeidInntektYtelseService.(List<Arbeidsforhold>, List<Inntekt.Lønn>, List<Inntekt.Lønn>) -> R) =
            lønnsinntekter.partition { inntekt ->
                arbeidsforholdliste.any { arbeidsforhold ->
                    inntekt.virksomhet == arbeidsforhold.arbeidsgiver
                }
            }.let { fordeltInntekter ->
                val arbeidsforholdUtenInntekter = arbeidsforholdliste.filter { arbeidsforhold ->
                    lønnsinntekter.none { inntekt ->
                        arbeidsforhold.arbeidsgiver == inntekt.virksomhet
                    }
                }

                callback(this, arbeidsforholdUtenInntekter, fordeltInntekter.first, fordeltInntekter.second)
            }

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

    private fun grupperInntekterEtterArbeidsforholdOgPeriode(inntekter: List<Inntekt.Lønn>, arbeidsforholdliste: List<Arbeidsforhold>) =
            inntekter.groupBy { inntekt ->
                inntekt.virksomhet
            }.mapKeys { entry ->
                arbeidsforholdliste.first { arbeidsforhold ->
                    entry.key.identifikator == arbeidsforhold.arbeidsgiver.identifikator
                }
            }.mapValues { entry ->
                entry.value.groupBy { lønnsinntekt ->
                    lønnsinntekt.utbetalingsperiode
                }
            }

    private fun tellAvvikPåArbeidsforhold(arbeidsforholdliste: List<Arbeidsforhold>) =
            arbeidsforholdliste.forEach { arbeidsforhold ->
                arbeidsforholdAvviksCounter.labels(arbeidsforhold.type()).inc()
                log.warn("did not find inntekter for arbeidsforhold (${arbeidsforhold.type()}) with arbeidsgiver=${arbeidsforhold.arbeidsgiver}")
            }

    private fun tellAvvikPåInntekter(inntekter: List<Inntekt.Lønn>) {
        inntektAvviksCounter.inc(inntekter.size.toDouble())
        log.warn("did not find arbeidsforhold for ${inntekter.size} inntekter: ${inntekter.joinToString { "${it.type()} - ${it.virksomhet}" }}" )
    }

    private inline fun <reified T: Inntekt> grupperInntektPåType(inntekter: Map<Class<out Inntekt>, Map<Virksomhet, List<Inntekt>>>) =
            inntekter[T::class.java].orEmpty().mapValues { inntektEtterVirksomhet ->
                inntektEtterVirksomhet.value.map { inntekt ->
                    inntekt as T
                }
            }
}
