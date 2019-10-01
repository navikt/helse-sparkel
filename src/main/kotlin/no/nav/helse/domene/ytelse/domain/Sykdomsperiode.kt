package no.nav.helse.domene.ytelse.domain

import java.time.LocalDate

data class Sykdomsperiode(val fom: LocalDate,
                          val tom: LocalDate,
                          val grad: Int)
