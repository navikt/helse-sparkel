package no.nav.helse.ws.aiy.domain

import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold
import no.nav.helse.ws.inntekt.domain.Inntekt

data class ArbeidsforholdMedInntekt(val arbeidsforhold: Arbeidsforhold, val inntekter: List<Inntekt>)
