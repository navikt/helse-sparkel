package no.nav.helse.ws.aiy.domain

import no.nav.helse.ws.inntekt.domain.Inntekt
import no.nav.helse.ws.inntekt.domain.Virksomhet
import java.time.YearMonth

data class ArbeidInntektYtelse(val arbeidsforhold: Map<Arbeidsforhold, Map<YearMonth, List<Inntekt.Lønn>>> = emptyMap(),
                               val ytelser: Map<Virksomhet, List<Inntekt.Ytelse>> = emptyMap(),
                               val pensjonEllerTrygd: Map<Virksomhet, List<Inntekt.PensjonEllerTrygd>> = emptyMap(),
                               val næringsinntekt: Map<Virksomhet, List<Inntekt.Næring>> = emptyMap())
