package no.nav.helse.ws.inntekt.domain

import java.math.BigDecimal
import java.time.YearMonth

sealed class Inntekt(open val virksomhet: Virksomhet, open val utbetalingsperiode: YearMonth, open val beløp: BigDecimal) {

    fun isYtelse() = when (this) {
        is Ytelse -> true
        is PensjonEllerTrygd -> true
        else -> false
    }

    fun kode() = when (this) {
        is Ytelse -> kode
        is PensjonEllerTrygd -> kode
        is Næring -> kode
        else -> null
    }

    data class Ytelse(override val virksomhet: Virksomhet, override val utbetalingsperiode: YearMonth, override val beløp: BigDecimal, val kode: String): Inntekt(virksomhet, utbetalingsperiode, beløp)
    data class PensjonEllerTrygd(override val virksomhet: Virksomhet, override val utbetalingsperiode: YearMonth, override val beløp: BigDecimal, val kode: String): Inntekt(virksomhet, utbetalingsperiode, beløp)
    data class Næring(override val virksomhet: Virksomhet, override val utbetalingsperiode: YearMonth, override val beløp: BigDecimal, val kode: String): Inntekt(virksomhet, utbetalingsperiode, beløp)
    data class Lønn(override val virksomhet: Virksomhet, override val utbetalingsperiode: YearMonth, override val beløp: BigDecimal): Inntekt(virksomhet, utbetalingsperiode, beløp)
}
