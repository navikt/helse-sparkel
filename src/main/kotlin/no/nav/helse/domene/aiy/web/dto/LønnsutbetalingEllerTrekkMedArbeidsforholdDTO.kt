package no.nav.helse.domene.aiy.web.dto

data class LønnsutbetalingEllerTrekkMedArbeidsforholdDTO(val inntekt: LønnsutbetalingEllerTrekkDTO, val muligeArbeidsforhold: List<ArbeidsforholdDTO>)
