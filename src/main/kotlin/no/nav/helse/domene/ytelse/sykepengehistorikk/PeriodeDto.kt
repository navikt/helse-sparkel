package no.nav.helse.domene.ytelse.sykepengehistorikk

import java.time.LocalDate

data class PeriodeDto(val fom: LocalDate, val tom: LocalDate, val grad: String)
