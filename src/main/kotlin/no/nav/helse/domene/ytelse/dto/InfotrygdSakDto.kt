package no.nav.helse.domene.ytelse.dto

import java.time.LocalDate

data class InfotrygdSakDto(
        val sakId: String,
        val iverksatt: LocalDate?,
        val tema: String,
        val behandlingstema: String,
        val opphørerFom: LocalDate?
)
