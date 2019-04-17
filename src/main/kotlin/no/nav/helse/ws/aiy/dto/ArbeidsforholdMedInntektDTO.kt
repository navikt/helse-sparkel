package no.nav.helse.ws.aiy.dto

import no.nav.helse.ws.arbeidsforhold.dto.ArbeidsforholdDTO
import java.time.YearMonth

data class ArbeidsforholdMedInntektDTO(val arbeidsforhold: ArbeidsforholdDTO, val perioder: Map<YearMonth, InntektsperiodeDTO>)
