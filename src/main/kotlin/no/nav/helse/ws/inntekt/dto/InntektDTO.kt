package no.nav.helse.ws.inntekt.dto

import no.nav.helse.ws.inntekt.domain.Opptjeningsperiode
import java.math.BigDecimal

data class InntektDTO(val arbeidsgiver: ArbeidsgiverDTO, val opptjeningsperiode: Opptjeningsperiode, val beløp: BigDecimal, val ytelse: Boolean, val kode: String?)
