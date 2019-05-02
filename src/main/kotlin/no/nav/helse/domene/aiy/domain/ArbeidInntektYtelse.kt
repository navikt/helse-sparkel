package no.nav.helse.domene.aiy.domain

import no.nav.helse.domene.arbeid.domain.Arbeidsforhold
import no.nav.helse.domene.inntekt.domain.Inntekt

data class ArbeidInntektYtelse(val lønnsinntekter: List<Pair<Inntekt.Lønn, List<Arbeidsforhold>>> = emptyList(),
                               val arbeidsforhold: List<Arbeidsforhold> = emptyList(),
                               val ytelser: List<Inntekt.Ytelse> = emptyList(),
                               val pensjonEllerTrygd: List<Inntekt.PensjonEllerTrygd> = emptyList(),
                               val næringsinntekt: List<Inntekt.Næring> = emptyList())
