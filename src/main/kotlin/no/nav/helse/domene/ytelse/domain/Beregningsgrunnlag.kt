package no.nav.helse.domene.ytelse.domain

import java.time.LocalDate

sealed class Beregningsgrunnlag(val identdato: LocalDate,
                                val periodeFom: LocalDate?,
                                val periodeTom: LocalDate?,
                                val behandlingstema: Behandlingstema,
                                val vedtak: List<BeregningsgrunnlagVedtak>) {

    fun hørerSammenMed(sak: InfotrygdSak) =
            identdato == sak.iverksatt && behandlingstema.tema == sak.tema

    fun type() = when (this) {
        is Sykepenger -> "Sykepenger"
        is Foreldrepenger -> "Foreldrepenger"
        is Engangstønad -> "Engangstønad"
        is PårørendeSykdom -> "PårørendeSykdom"
    }

    class Sykepenger(identdato: LocalDate,
                     periodeFom: LocalDate?,
                     periodeTom: LocalDate?,
                     behandlingstema: Behandlingstema,
                     vedtak: List<BeregningsgrunnlagVedtak>): Beregningsgrunnlag(identdato, periodeFom, periodeTom, behandlingstema, vedtak) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false
            return true
        }

        override fun toString(): String {
            return "Sykepenger(identdato=$identdato, periodeFom=$periodeFom, periodeTom=$periodeTom, behandlingstema=$behandlingstema, vedtak=$vedtak)"
        }
    }

    class Foreldrepenger(identdato: LocalDate,
                         periodeFom: LocalDate?,
                         periodeTom: LocalDate?,
                         behandlingstema: Behandlingstema,
                         vedtak: List<BeregningsgrunnlagVedtak>): Beregningsgrunnlag(identdato, periodeFom, periodeTom, behandlingstema, vedtak) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false
            return true
        }

        override fun toString(): String {
            return "Foreldrepenger(identdato=$identdato, periodeFom=$periodeFom, periodeTom=$periodeTom, behandlingstema=$behandlingstema, vedtak=$vedtak)"
        }
    }

    class Engangstønad(identdato: LocalDate,
                       periodeFom: LocalDate?,
                       periodeTom: LocalDate?,
                       behandlingstema: Behandlingstema,
                       vedtak: List<BeregningsgrunnlagVedtak>): Beregningsgrunnlag(identdato, periodeFom, periodeTom, behandlingstema, vedtak) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false
            return true
        }

        override fun toString(): String {
            return "Engangstønad(identdato=$identdato, periodeFom=$periodeFom, periodeTom=$periodeTom, behandlingstema=$behandlingstema, vedtak=$vedtak)"
        }
    }

    class PårørendeSykdom(identdato: LocalDate,
                          periodeFom: LocalDate?,
                          periodeTom: LocalDate?,
                          behandlingstema: Behandlingstema,
                          vedtak: List<BeregningsgrunnlagVedtak>): Beregningsgrunnlag(identdato, periodeFom, periodeTom, behandlingstema, vedtak) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false
            return true
        }

        override fun toString(): String {
            return "PårørendeSykdom(identdato=$identdato, periodeFom=$periodeFom, periodeTom=$periodeTom, behandlingstema=$behandlingstema, vedtak=$vedtak)"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Beregningsgrunnlag

        if (identdato != other.identdato) return false
        if (periodeFom != other.periodeFom) return false
        if (periodeTom != other.periodeTom) return false
        if (behandlingstema != other.behandlingstema) return false
        if (vedtak != other.vedtak) return false

        return true
    }

    override fun hashCode(): Int {
        var result = identdato.hashCode()
        result = 31 * result + (periodeFom?.hashCode() ?: 0)
        result = 31 * result + (periodeTom?.hashCode() ?: 0)
        result = 31 * result + behandlingstema.hashCode()
        result = 31 * result + vedtak.hashCode()
        return result
    }
}

data class BeregningsgrunnlagVedtak(val fom: LocalDate,
                                    val tom: LocalDate,
                                    val utbetalingsgrad: Int)
