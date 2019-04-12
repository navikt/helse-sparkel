package no.nav.helse.ws.person.dto

import no.nav.helse.ws.person.domain.Kjønn
import java.time.LocalDate

data class PersonDTO(
        val aktørId: String,
        val fornavn: String,
        val mellomnavn: String? = null,
        val etternavn: String,
        val fdato: LocalDate,
        val kjønn: Kjønn,
        val statsborgerskap: String?,
        val status: String?,
        val bostedsland: String?,
        val diskresjonskode: String?
)
