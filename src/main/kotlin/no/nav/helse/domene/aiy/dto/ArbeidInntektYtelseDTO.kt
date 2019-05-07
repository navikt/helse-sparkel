package no.nav.helse.domene.aiy.dto

import no.nav.helse.domene.arbeid.dto.ArbeidsforholdDTO

data class ArbeidInntektYtelseDTO(val arbeidsforhold: List<ArbeidsforholdDTO>,
                                  val inntekter: List<LønnsutbetalingEllerTrekkMedArbeidsforholdDTO>,
                                  val ytelser: List<YtelseutbetalingEllerTrekkDTO>,
                                  val pensjonEllerTrygd: List<PensjonEllerTrygdUtbetalingEllerTrekkDTO>,
                                  val næring: List<NæringsutbetalingEllerTrekkDTO>)
