package no.nav.helse.ws.aiy.dto

import java.time.YearMonth

data class ArbeidsforholdMedInntektDTO(val arbeidsforhold: ArbeidsforholdDTO, val perioder: Map<YearMonth, InntektsperiodeDTO>)
