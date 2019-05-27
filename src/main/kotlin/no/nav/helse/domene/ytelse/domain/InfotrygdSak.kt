package no.nav.helse.domene.ytelse.domain

import java.time.LocalDate

data class InfotrygdSak(
        val sakId: String?,
        val iverksatt: LocalDate?,
        val tema: Tema,
        val behandlingstema: Behandlingstema,
        val opphørerFom: LocalDate?
)
