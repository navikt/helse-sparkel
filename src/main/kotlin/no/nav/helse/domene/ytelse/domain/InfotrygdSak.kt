package no.nav.helse.domene.ytelse.domain

import java.time.LocalDate
import java.util.*

sealed class InfotrygdSak(val tema: Tema,
                          val behandlingstema: Behandlingstema) {

    class Sak(tema: Tema,
              behandlingstema: Behandlingstema): InfotrygdSak(tema, behandlingstema) {
        override fun toString(): String {
            return "InfotrygdSak.Sak(tema=$tema, behandlingstema=$behandlingstema)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false
            return true
        }

        override fun hashCode(): Int {
            return super.hashCode() * 31
        }
    }

    class Vedtak(tema: Tema,
                 behandlingstema: Behandlingstema,
                 val iverksatt: LocalDate?,
                 val opphørerFom: LocalDate?): InfotrygdSak(tema, behandlingstema) {
        override fun toString(): String {
            return "InfotrygdSak.Vedtak(tema=$tema, behandlingstema=$behandlingstema, iverksatt=$iverksatt, opphørerFom=$opphørerFom)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as InfotrygdSak.Vedtak

            if (iverksatt != other.iverksatt) return false

            return opphørerFom == other.opphørerFom
        }

        override fun hashCode(): Int {
            var hash = 31 * super.hashCode() + Objects.hash(iverksatt)
            return 31 * hash + Objects.hash(opphørerFom)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InfotrygdSak

        if (tema != other.tema) return false
        if (behandlingstema != other.behandlingstema) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(tema, behandlingstema)
    }
}
