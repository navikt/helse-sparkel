package no.nav.helse.domene.ytelse.domain

import java.time.LocalDate

sealed class Utbetalingsvedtak(val fom: LocalDate,
                               val tom: LocalDate) {

    init {
        if (fom > tom) {
            throw IllegalArgumentException("$fom er nyere dato enn $tom")
        }
    }

    class SkalIkkeUtbetales(fom: LocalDate,
                            tom: LocalDate): Utbetalingsvedtak(fom, tom) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result *= 31
            return result
        }

        override fun toString(): String {
            return "Utbetalingsvedtak.SkalIkkeUtbetales(fom=$fom, tom=$tom)"
        }
    }

    class SkalUtbetales(fom: LocalDate,
                        tom: LocalDate,
                        val utbetalingsgrad: Int): Utbetalingsvedtak(fom, tom) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as SkalUtbetales

            return utbetalingsgrad == other.utbetalingsgrad
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + utbetalingsgrad.hashCode()
            return result
        }

        override fun toString(): String {
            return "Utbetalingsvedtak.SkalUtbetales(fom=$fom, tom=$tom, utbetalingsgrad=$utbetalingsgrad)"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Utbetalingsvedtak

        if (fom != other.fom) return false
        if (tom != other.tom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fom.hashCode()
        result = 31 * result + tom.hashCode()
        return result
    }
}
