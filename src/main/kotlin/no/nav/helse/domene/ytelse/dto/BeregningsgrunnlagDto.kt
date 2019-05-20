package no.nav.helse.domene.ytelse.dto

import java.time.LocalDate

data class BeregningsgrunnlagDto(val type: String,
                                 val identdato: LocalDate,
                                 val periodeFom: LocalDate?,
                                 val periodeTom: LocalDate?,
                                 val behandlingstema: String,
                                 val vedtak: List<BeregningsgrunnlagVedtakDto>)

