package no.nav.helse.domene.aiy.domain

import java.math.BigDecimal
import java.time.LocalDate

sealed class Arbeidsavtale(val yrke: String,
                           val stillingsprosent: BigDecimal?,
                           val fom: LocalDate) {

    class Gjeldende(yrke: String,
                    stillingsprosent: BigDecimal?,
                    fom: LocalDate) : Arbeidsavtale(yrke, stillingsprosent, fom) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            return other is Gjeldende
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result *= 31
            return result
        }

        override fun toString(): String {
            return "Arbeidsavtale.Gjeldende(yrke='$yrke', stillingsprosent=$stillingsprosent, fom=$fom)"
        }
    }

    class Historisk(yrke: String,
                    stillingsprosent: BigDecimal?,
                    fom: LocalDate,
                    val tom: LocalDate) : Arbeidsavtale(yrke, stillingsprosent, fom) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as Historisk

            if (tom != other.tom) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + tom.hashCode()
            return result
        }

        override fun toString(): String {
            return "Arbeidsavtale.Historisk(yrke='$yrke', stillingsprosent=$stillingsprosent, fom=$fom, tom=$tom)"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Arbeidsavtale

        if (yrke != other.yrke) return false
        if (stillingsprosent != other.stillingsprosent) return false
        if (fom != other.fom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = yrke.hashCode()
        result = 31 * result + (stillingsprosent?.hashCode() ?: 0)
        result = 31 * result + fom.hashCode()
        return result
    }
}
