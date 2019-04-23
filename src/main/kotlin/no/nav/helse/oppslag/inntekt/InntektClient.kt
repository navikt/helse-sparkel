package no.nav.helse.oppslag.inntekt

import arrow.core.Try
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.AktørId
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Ainntektsfilter
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.AktoerId
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Formaal
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Uttrekksperiode
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest
import org.slf4j.LoggerFactory
import java.time.YearMonth


class InntektClient(private val inntektV3: InntektV3) {

    companion object {
        private val log = LoggerFactory.getLogger(InntektClient::class.java)
    }

    fun hentInntekter(aktørId: AktørId, fom: YearMonth, tom: YearMonth, filter: String) =
            Try {
                inntektV3.hentInntektListeBolk(hentInntektListeRequest(aktørId, fom, tom, filter))
            }.flatMap { response ->
                if (response.sikkerhetsavvikListe != null && response.sikkerhetsavvikListe.isNotEmpty()) {
                    response.sikkerhetsavvikListe.joinToString {
                        it.tekst
                    }.let { feilmelding ->
                        log.error("Sikkerhetsavvik fra inntekt: $feilmelding")
                        Try.Failure(SikkerhetsavvikException(feilmelding))
                    }
                } else {
                    Try.Success(response.arbeidsInntektIdentListe)
                }
            }

    fun hentFrilansArbeidsforhold(aktørId: AktørId, fom: YearMonth, tom: YearMonth) =
            hentInntekter(aktørId, fom, tom, "ForeldrepengerA-Inntekt").map {
                it.flatMap {
                    it.arbeidsInntektMaaned
                }.flatMap {
                    it.arbeidsInntektInformasjon.arbeidsforholdListe
                }
            }

    private fun hentInntektListeRequest(aktørId: AktørId, fom: YearMonth, tom: YearMonth, filter: String) =
            HentInntektListeBolkRequest().apply {
                identListe.add(AktoerId().apply {
                    aktoerId = aktørId.aktor
                })
                formaal = Formaal().apply {
                    value = "Foreldrepenger"
                }
                ainntektsfilter = Ainntektsfilter().apply {
                    value = filter
                }
                uttrekksperiode = Uttrekksperiode().apply {
                    maanedFom = fom.toXmlGregorianCalendar()
                    maanedTom = tom.toXmlGregorianCalendar()
                }
            }
}

