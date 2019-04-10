package no.nav.helse.ws.aiy.dto

import java.time.YearMonth

data class ArbeidsforholdMedInntektDTO(val arbeidsforhold: ArbeidsforholdDTO, val inntekter: Map<YearMonth, List<InntektDTO>>)
