package no.nav.helse.ws.aiy

import no.nav.helse.ws.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.ws.aiy.dto.*
import no.nav.helse.ws.arbeidsforhold.ArbeidDtoMapper

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
                            ArbeidInntektYtelseDTO(arbeidsforhold, ytelser, pensjonEllerTrygd, næring)
                        }
                    }
                }
            }

}
