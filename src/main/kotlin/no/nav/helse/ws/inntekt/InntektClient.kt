package no.nav.helse.ws.inntekt

import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.YearMonth
import javax.xml.datatype.DatatypeFactory


class InntektClient(private val inntektV3: InntektV3) {
    private val log = LoggerFactory.getLogger(InntektClient::class.java)

    fun hentInntektListe(fødelsnummer: String): OppslagResult {
        val request = HentInntektListeBolkRequest().apply {
            identListe.add(PersonIdent().apply {
                personIdent = fødelsnummer
            })
            formaal = Formaal().apply {
                value = "Sykepenger"
            }
            ainntektsfilter = Ainntektsfilter().apply {
                value = "SykepengerA-Inntekt"
            }
            uttrekksperiode = Uttrekksperiode().apply {
                maanedFom = DatatypeFactory.newInstance().newXMLGregorianCalendar(2017, 12, 1, 0, 0, 0, 0, 0)
                maanedTom = DatatypeFactory.newInstance().newXMLGregorianCalendar(2018, 1, 1, 0, 0, 0, 0, 0)
            }
        }

        try {
            val response = inntektV3.hentInntektListeBolk(request)

            return Success(HentInntektListeResponse(
                    response.arbeidsInntektIdentListe[0].arbeidsInntektMaaned.map {
                        Inntektsperiode(
                                it.aarMaaned.let { YearMonth.of(it.year, it.month) },
                                it.avvikListe.map { Avvik(it.ident,
                                        it.opplysningspliktig,
                                        it.virksomhet,
                                        it.avvikPeriode.let { YearMonth.of(it.year, it.month) },
                                        it.tekst
                                ) },
                                it.arbeidsInntektInformasjon.arbeidsforholdListe,
                                it.arbeidsInntektInformasjon.inntektListe.map {
                                    val beskrivelse = when(it) {
                                        is Loennsinntekt -> it.beskrivelse.value
                                        is PensjonEllerTrygd -> it.beskrivelse.value
                                        is YtelseFraOffentlige -> it.beskrivelse.value
                                        is Naeringsinntekt -> it.beskrivelse.value
                                        else -> "ukjent"
                                    }
                                    Inntekt(
                                            it.isUtloeserArbeidsgiveravgift,
                                            it.opplysningspliktig,
                                            it.informasjonsstatus.value,
                                            it.virksomhet,
                                            it.beloep,
                                            it.inntektsstatus.value,
                                            it.isInngaarIGrunnlagForTrekk,
                                            it.inntektsperiodetype.value,
                                            it.fordel.value,
                                            beskrivelse,
                                            it.inntektskilde.value,
                                            it.utbetaltIPeriode.toXMLFormat()
                                    )
                                },
                                it.arbeidsInntektInformasjon.forskuddstrekkListe,
                                it.arbeidsInntektInformasjon.fradragListe
                        )
                    }
            ))
        } catch (ex: Exception) {
            log.error("Error during inntekt lookup", ex)
            return Failure(listOf(ex.message ?: "unknown error"))
        }
    }

    data class HentInntektListeResponse(
            val perioder: List<Inntektsperiode>
    )
    data class Inntektsperiode(
            val årMåned: YearMonth,
            val avvik: List<Avvik>,
            val arbeidsforhold: List<ArbeidsforholdFrilanser>,
            val inntekter: List<Inntekt>,
            val forskuddstrekk: List<Forskuddstrekk>,
            val fradrag: List<Fradrag>
    ) {
        fun getÅrMåned(): String {
            return årMåned.toString()
        }
    }
    data class Inntekt(
            val utloeserArbeidsgiveravgift: Boolean,
            val opplysningspliktig: Aktoer,
            val informasjonsstatus: String,
            val virksomhet: Aktoer,
            val beløp: BigDecimal,
            val inntektsstatus: String,
            val inngaarIGrunnlagForTrekk: Boolean,
            val inntektsperiodetype: String,
            val fordel: String,
            val beskrivelse: String,
            val inntektskilde: String,
            val utbetaltIPeriode: String
    )

    data class Avvik(
            val ident: Aktoer,
            val opplysningspliktig: Aktoer,
            val virksomhet: Aktoer,
            val avvikPeriode: YearMonth,
            val tekst: String
    )
}

