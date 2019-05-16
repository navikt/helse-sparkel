package no.nav.helse.domene.ytelse.domain

import java.time.LocalDate

data class Ytelse(val tema: String, val fom: LocalDate, val tom: LocalDate)
