package no.nav.helse.domene.aiy.dto

import no.nav.helse.domene.arbeid.dto.ArbeidsforholdDTO

data class InntektMedArbeidsforholdDTO(val inntekt: InntektDTO, val muligeArbeidsforhold: List<ArbeidsforholdDTO>)
