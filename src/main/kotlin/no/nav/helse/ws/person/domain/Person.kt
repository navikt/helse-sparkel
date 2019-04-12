package no.nav.helse.ws.person.domain

import no.nav.helse.ws.AktørId
import java.time.LocalDate

data class Person(
        val id: AktørId,
        val fornavn: String,
        val mellomnavn: String? = null,
        val etternavn: String,
        val fdato: LocalDate,
        val kjønn: Kjønn,
        val statsborgerskap: String? = null,
        val status: String? = null,
        val bostedsland: String? = null,
        val diskresjonskode: String? = null
)
