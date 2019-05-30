package no.nav.helse.domene.aiy.web.dto

import java.time.LocalDate

data class ArbeidsforholdDTO(val type: String,
                             val arbeidsgiver: ArbeidsgiverDTO,
                             val startdato: LocalDate,
                             val sluttdato: LocalDate? = null,
                             val yrke: String?,
                             val arbeidsavtaler: List<ArbeidsavtaleDTO> = emptyList(),
                             val permisjon: List<PermisjonDTO> = emptyList())
