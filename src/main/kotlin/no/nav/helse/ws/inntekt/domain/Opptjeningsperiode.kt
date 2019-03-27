package no.nav.helse.ws.inntekt.domain

import java.time.LocalDate

data class Opptjeningsperiode(val fom: LocalDate, val tom: LocalDate, val antattPeriode: Boolean = false) {
    init {
        if (fom.withDayOfMonth(1) != tom.withDayOfMonth(1)) {
            throw OpptjeningsperiodeStrekkerSegOverEnKalendermånedException("Opptjeningsperiode kan ikke strekke seg over flere måneder")
        }
        if (tom < fom) {
            throw FomErStørreEnTomException("tom < fom")
        }
    }
}
