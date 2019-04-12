package no.nav.helse.ws.aiy.dto

import no.nav.helse.ws.arbeidsforhold.dto.ArbeidsavtaleDTO
import no.nav.helse.ws.arbeidsforhold.dto.PermisjonDTO
import no.nav.helse.ws.inntekt.dto.ArbeidsgiverDTO
import java.time.LocalDate

data class ArbeidsforholdDTO(val type: String,
                             val arbeidsgiver: ArbeidsgiverDTO,
                             val startdato: LocalDate,
                             val sluttdato: LocalDate? = null,
                             val yrke: String?,
                             val arbeidsavtaler: List<ArbeidsavtaleDTO> = emptyList(),
                             val permisjon: List<PermisjonDTO> = emptyList())
