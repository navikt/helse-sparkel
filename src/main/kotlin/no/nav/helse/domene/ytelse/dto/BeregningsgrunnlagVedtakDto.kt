package no.nav.helse.domene.ytelse.dto

import java.time.LocalDate

data class BeregningsgrunnlagVedtakDto(val fom: LocalDate,
                                       val tom: LocalDate,
                                       val utbetalingsgrad: Int?)
