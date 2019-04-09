package no.nav.helse.ws.aiy.domain

import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold
import no.nav.helse.ws.inntekt.domain.Inntekt
import no.nav.helse.ws.inntekt.domain.Virksomhet

data class ArbeidInntektYtelse(val arbeidsforhold: Map<Arbeidsforhold, List<Inntekt.Lønn>> = emptyMap(),
                               val ytelser: Map<Virksomhet, List<Inntekt.Ytelse>> = emptyMap(),
                               val pensjonEllerTrygd: Map<Virksomhet, List<Inntekt.PensjonEllerTrygd>> = emptyMap(),
                               val næringsinntekt: Map<Virksomhet, List<Inntekt.Næring>> = emptyMap())
