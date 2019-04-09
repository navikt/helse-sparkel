package no.nav.helse.ws.aiy

import no.nav.helse.ws.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.ws.aiy.dto.*
import no.nav.helse.ws.arbeidsforhold.ArbeidDtoMapper
import no.nav.helse.ws.inntekt.InntektDtoMapper

object ArbeidsInntektYtelseDtoMapper {

    fun toDto(arbeidInntektYtelse: ArbeidInntektYtelse) =
            arbeidInntektYtelse.arbeidsforhold.map { arbeidsforhold ->
                arbeidsforhold.value.map { inntekt ->
                    InntektUtenArbeidsgiverDTO(inntekt.utbetalingsperiode, inntekt.beløp)
                }.let {
                    ArbeidsforholdMedInntektDTO(
                            arbeidsforhold = ArbeidDtoMapper.toDto(arbeidsforhold.key),
                            inntekter = it
                    )
                }
            }.let { arbeidsforhold ->
                arbeidInntektYtelse.frilans.map { frilansArbeidsforhold ->
                    frilansArbeidsforhold.value.map { inntekt ->
                        InntektUtenArbeidsgiverDTO(inntekt.utbetalingsperiode, inntekt.beløp)
                    }.let {
                        FrilansArbeidsforholdMedInntektDTO(
                                arbeidsforhold = FrilansArbeidsforholdDTO(
                                        arbeidsgiver = InntektDtoMapper.toDto(frilansArbeidsforhold.key.arbeidsgiver),
                                        yrke = frilansArbeidsforhold.key.yrke,
                                        startdato = frilansArbeidsforhold.key.startdato,
                                        sluttdato = frilansArbeidsforhold.key.sluttdato
                                ),
                                inntekter = it
                        )
                    }
                }.let { frilansArbeidsforhold ->
                    arbeidInntektYtelse.ytelser.flatMap { ytelse ->
                        ytelse.value.map {
                            YtelseDTO(it.virksomhet, it.utbetalingsperiode, it.beløp, it.kode)
                        }
                    }.let { ytelser ->
                        arbeidInntektYtelse.pensjonEllerTrygd.flatMap { pensjonEllerTrygd ->
                            pensjonEllerTrygd.value.map {
                                PensjonEllerTrygdDTO(it.virksomhet, it.utbetalingsperiode, it.beløp, it.kode)
                            }
                        }.let { pensjonEllerTrygd ->
                            arbeidInntektYtelse.næringsinntekt.flatMap { næring ->
                                næring.value.map {
                                    NæringDTO(it.virksomhet, it.utbetalingsperiode, it.beløp, it.kode)
                                }
                            }.let { næring ->
                                ArbeidInntektYtelseDTO(arbeidsforhold, frilansArbeidsforhold, ytelser, pensjonEllerTrygd, næring)
                            }
                        }
                    }
                }
            }

}
