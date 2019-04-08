package no.nav.helse.ws.aiy.dto

import no.nav.helse.ws.arbeidsforhold.dto.ArbeidsforholdDTO

data class ArbeidsforholdMedInntektDTO(val arbeidsforhold: ArbeidsforholdDTO, val inntekter: List<InntektUtenArbeidsgiverDTO>)
