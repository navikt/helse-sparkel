package no.nav.helse.ws.inntekt.domain

import java.time.LocalDate

data class ArbeidsforholdFrilanser(
        val arbeidsgiver: Arbeidsgiver,
        val startdato: LocalDate?,
        val sluttdato: LocalDate?,
        val yrke: String?
)
