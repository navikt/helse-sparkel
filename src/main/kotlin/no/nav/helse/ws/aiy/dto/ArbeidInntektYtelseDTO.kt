package no.nav.helse.ws.aiy.dto

data class ArbeidInntektYtelseDTO(val arbeidsforhold: List<ArbeidsforholdMedInntektDTO>,
                                  val ytelser: List<YtelseDTO>,
                                  val pensjonEllerTrygd: List<PensjonEllerTrygdDTO>,
                                  val næring: List<NæringDTO>)
