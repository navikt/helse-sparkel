package no.nav.helse.ws.aiy.dto

import no.nav.helse.ws.arbeidsforhold.dto.ArbeidsforholdDTO

data class ArbeidInntektYtelseDTO(val arbeidsforhold: List<ArbeidsforholdMedInntektDTO>,
                                  val inntekterUtenArbeidsforhold: List<InntektMedArbeidsgiverDTO>,
                                  val arbeidsforholdUtenInntekter: List<ArbeidsforholdDTO>,
                                  val ytelser: List<YtelseDTO>,
                                  val pensjonEllerTrygd: List<PensjonEllerTrygdDTO>,
                                  val næring: List<NæringDTO>)
