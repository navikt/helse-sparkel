package no.nav.helse.ws.arbeidsforhold.dto

import java.time.LocalDate

data class ArbeidsforholdDTO(val arbeidsgiver: ArbeidsgiverDTO,
                             val startdato: LocalDate,
                             val sluttdato: LocalDate? = null,
                             val arbeidsavtaler: List<ArbeidsavtaleDTO> = emptyList(),
                             val permisjon: List<PermisjonDTO> = emptyList())
