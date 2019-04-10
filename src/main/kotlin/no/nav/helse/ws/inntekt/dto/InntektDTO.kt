package no.nav.helse.ws.inntekt.dto

import no.nav.helse.ws.inntekt.domain.Opptjeningsperiode
import java.math.BigDecimal
import java.time.YearMonth

data class InntektDTO(val arbeidsgiver: ArbeidsgiverDTO, val utbetalingsperiode: YearMonth, val opptjeningsperiode: Opptjeningsperiode, val beløp: BigDecimal, val ytelse: Boolean, val kode: String?)
