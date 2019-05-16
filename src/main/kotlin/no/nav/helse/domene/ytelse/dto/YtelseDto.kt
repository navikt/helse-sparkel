package no.nav.helse.domene.ytelse.dto

import java.time.LocalDate

data class YtelseDto(
        val kilde: String,
        val tema: String,
        val fom: LocalDate?,
        val tom: LocalDate?
)
