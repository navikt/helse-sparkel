package no.nav.helse.ws.aiy.domain

import no.nav.helse.ws.inntekt.domain.Inntekt
import java.time.YearMonth

data class ArbeidInntektYtelse(val arbeidsforhold: Map<Arbeidsforhold, Map<YearMonth, List<Inntekt.Lønn>>> = emptyMap(),
                               val inntekterUtenArbeidsforhold: List<Inntekt.Lønn> = emptyList(),
                               val arbeidsforholdUtenInntekter: List<Arbeidsforhold> = emptyList(),
                               val ytelser: List<Inntekt.Ytelse> = emptyList(),
                               val pensjonEllerTrygd: List<Inntekt.PensjonEllerTrygd> = emptyList(),
                               val næringsinntekt: List<Inntekt.Næring> = emptyList())
