package no.nav.helse.ws.aiy.domain

import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold

data class ArbeidsforholdMedInntekt(val arbeidsforhold: Arbeidsforhold, val inntekter: List<InntektUtenArbeidsgiver>)
