package no.nav.helse.domene.aiy.dto

import no.nav.helse.domene.inntekt.domain.Virksomhet
import java.math.BigDecimal
import java.time.YearMonth

data class PensjonEllerTrygdDTO(val virksomhet: Virksomhet, val utbetalingsperiode: YearMonth, val beløp: BigDecimal, val kode: String)
