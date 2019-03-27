package no.nav.helse.ws.inntekt.domain

import java.math.BigDecimal

data class Inntekt(val virksomhet: Virksomhet, val opptjeningsperiode: Opptjeningsperiode, val beløp: BigDecimal)
