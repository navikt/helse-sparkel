package no.nav.helse.domene.aiy.dto

import no.nav.helse.domene.arbeid.dto.ArbeidsforholdDTO

data class ArbeidInntektYtelseDTO(val arbeidsforhold: List<ArbeidsforholdDTO>,
                                  val inntekter: List<InntektMedArbeidsforholdDTO>,
                                  val ytelser: List<YtelseDTO>,
                                  val pensjonEllerTrygd: List<PensjonEllerTrygdDTO>,
                                  val næring: List<NæringDTO>)
