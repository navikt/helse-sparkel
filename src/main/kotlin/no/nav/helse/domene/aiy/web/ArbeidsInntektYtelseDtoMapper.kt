package no.nav.helse.domene.aiy.web

import no.nav.helse.domene.aiy.domain.ArbeidInntektYtelse
import no.nav.helse.domene.aiy.web.dto.*

object ArbeidsInntektYtelseDtoMapper {

    fun toDto(arbeidInntektYtelse: ArbeidInntektYtelse) =
            arbeidInntektYtelse.lønnsinntekter.map { inntektMedMuligeArbeidsforhold ->
                inntektMedMuligeArbeidsforhold.second.map(ArbeidDtoMapper::toDto).let { arbeidsforhold ->
                    LønnsutbetalingEllerTrekkMedArbeidsforholdDTO(inntektMedMuligeArbeidsforhold.first.let { inntekt ->
                        LønnsutbetalingEllerTrekkDTO(
                                virksomhet = UtbetalingEllerTrekkDtoMapper.toDto(inntekt.virksomhet),
                                utbetalingsperiode = inntekt.utbetalingsperiode,
                                beløp = inntekt.beløp)
                    }, arbeidsforhold)
                }
            }.let { inntekter ->
                arbeidInntektYtelse.arbeidsforhold.map(ArbeidDtoMapper::toDto).let { arbeidsforhold ->
                    arbeidInntektYtelse.ytelser.map {
                        YtelseutbetalingEllerTrekkDTO(UtbetalingEllerTrekkDtoMapper.toDto(it.virksomhet), it.utbetalingsperiode, it.beløp, it.kode)
                    }.let { ytelser ->
                        arbeidInntektYtelse.pensjonEllerTrygd.map {
                            PensjonEllerTrygdUtbetalingEllerTrekkDTO(UtbetalingEllerTrekkDtoMapper.toDto(it.virksomhet), it.utbetalingsperiode, it.beløp, it.kode)
                        }.let { pensjonEllerTrygd ->
                            arbeidInntektYtelse.næringsinntekt.map {
                                NæringsutbetalingEllerTrekkDTO(UtbetalingEllerTrekkDtoMapper.toDto(it.virksomhet), it.utbetalingsperiode, it.beløp, it.kode)
                            }.let { næring ->
                                ArbeidInntektYtelseDTO(arbeidsforhold, inntekter, ytelser, pensjonEllerTrygd, næring)
                            }
                        }
                    }
                }
            }

}
