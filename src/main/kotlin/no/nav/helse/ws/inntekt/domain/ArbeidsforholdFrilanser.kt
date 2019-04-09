package no.nav.helse.ws.inntekt.domain

import java.time.LocalDate

data class ArbeidsforholdFrilanser(
        val arbeidsgiver: Virksomhet,
        val startdato: LocalDate?,
        val sluttdato: LocalDate?,
        val yrke: String?
)
