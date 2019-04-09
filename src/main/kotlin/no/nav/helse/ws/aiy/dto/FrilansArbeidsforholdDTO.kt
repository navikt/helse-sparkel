package no.nav.helse.ws.aiy.dto

import java.time.LocalDate

data class FrilansArbeidsforholdDTO(val arbeidsgiver: no.nav.helse.ws.inntekt.dto.ArbeidsgiverDTO, val yrke: String?, val startdato: LocalDate? = null, val sluttdato: LocalDate? = null)
