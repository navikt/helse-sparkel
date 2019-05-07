package no.nav.helse.domene.aiy.dto

import no.nav.helse.domene.arbeid.dto.ArbeidsforholdDTO

data class LønnsutbetalingEllerTrekkMedArbeidsforholdDTO(val inntekt: LønnsutbetalingEllerTrekkDTO, val muligeArbeidsforhold: List<ArbeidsforholdDTO>)
