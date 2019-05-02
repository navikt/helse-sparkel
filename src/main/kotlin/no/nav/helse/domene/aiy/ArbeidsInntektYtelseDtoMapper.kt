package no.nav.helse.domene.aiy

import no.nav.helse.domene.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.domene.aiy.dto.*
import no.nav.helse.domene.arbeid.ArbeidDtoMapper
import no.nav.helse.domene.inntekt.InntektDtoMapper

object ArbeidsInntektYtelseDtoMapper {

    fun toDto(arbeidInntektYtelse: ArbeidInntektYtelse) =
            arbeidInntektYtelse.lønnsinntekter.map { inntektMedMuligeArbeidsforhold ->
                inntektMedMuligeArbeidsforhold.second.map(ArbeidDtoMapper::toDto).let { arbeidsforhold ->
                    InntektMedArbeidsforholdDTO(inntektMedMuligeArbeidsforhold.first.let { inntekt ->
                        InntektDTO(
                                virksomhet = InntektDtoMapper.toDto(inntekt.virksomhet),
                                utbetalingsperiode = inntekt.utbetalingsperiode,
                                beløp = inntekt.beløp)
                    }, arbeidsforhold)
                }
            }.let { inntekter ->
                arbeidInntektYtelse.arbeidsforhold.map(ArbeidDtoMapper::toDto).let { arbeidsforhold ->
                    arbeidInntektYtelse.ytelser.map {
                        YtelseDTO(it.virksomhet, it.utbetalingsperiode, it.beløp, it.kode)
                    }.let { ytelser ->
                        arbeidInntektYtelse.pensjonEllerTrygd.map {
                            PensjonEllerTrygdDTO(it.virksomhet, it.utbetalingsperiode, it.beløp, it.kode)
                        }.let { pensjonEllerTrygd ->
                            arbeidInntektYtelse.næringsinntekt.map {
                                NæringDTO(it.virksomhet, it.utbetalingsperiode, it.beløp, it.kode)
                            }.let { næring ->
                                ArbeidInntektYtelseDTO(arbeidsforhold, inntekter, ytelser, pensjonEllerTrygd, næring)
                            }
                        }
                    }
                }
            }

}
