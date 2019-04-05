package no.nav.helse.ws.arbeidsforhold.dto

import java.time.LocalDate

data class ArbeidsforholdDTO(val arbeidsgiver: ArbeidsgiverDTO, val startdato: LocalDate, val sluttdato: LocalDate? = null)
