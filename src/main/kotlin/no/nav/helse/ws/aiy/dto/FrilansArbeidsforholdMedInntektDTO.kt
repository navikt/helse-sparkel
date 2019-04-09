package no.nav.helse.ws.aiy.dto

import no.nav.helse.ws.arbeidsforhold.dto.ArbeidsforholdDTO

data class FrilansArbeidsforholdMedInntektDTO(val arbeidsforhold: FrilansArbeidsforholdDTO, val inntekter: List<InntektUtenArbeidsgiverDTO>)
