package no.nav.helse.ws.aiy.dto

data class ArbeidInntektYtelseDTO(val arbeidsforhold: List<ArbeidsforholdMedInntektDTO>,
                                  val frilansArbeidsforhold: List<FrilansArbeidsforholdMedInntektDTO>,
                                  val ytelser: List<YtelseDTO>,
                                  val pensjonEllerTrygd: List<PensjonEllerTrygdDTO>,
                                  val næring: List<NæringDTO>)
