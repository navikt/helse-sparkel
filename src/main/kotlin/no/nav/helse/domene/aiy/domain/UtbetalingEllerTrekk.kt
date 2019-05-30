package no.nav.helse.domene.aiy.domain

import java.math.BigDecimal
import java.time.YearMonth

sealed class UtbetalingEllerTrekk(open val virksomhet: Virksomhet, open val utbetalingsperiode: YearMonth, open val beløp: BigDecimal) {

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

    fun type() = when (this) {
        is Ytelse -> "Ytelse"
        is PensjonEllerTrygd -> "PensjonEllerTrygd"
        is Næring -> "Næring"
        is Lønn -> "Lønn"
    }

    data class Ytelse(override val virksomhet: Virksomhet, override val utbetalingsperiode: YearMonth, override val beløp: BigDecimal, val kode: String): UtbetalingEllerTrekk(virksomhet, utbetalingsperiode, beløp)
    data class PensjonEllerTrygd(override val virksomhet: Virksomhet, override val utbetalingsperiode: YearMonth, override val beløp: BigDecimal, val kode: String): UtbetalingEllerTrekk(virksomhet, utbetalingsperiode, beløp)
    data class Næring(override val virksomhet: Virksomhet, override val utbetalingsperiode: YearMonth, override val beløp: BigDecimal, val kode: String): UtbetalingEllerTrekk(virksomhet, utbetalingsperiode, beløp)
    data class Lønn(override val virksomhet: Virksomhet, override val utbetalingsperiode: YearMonth, override val beløp: BigDecimal): UtbetalingEllerTrekk(virksomhet, utbetalingsperiode, beløp)
}
