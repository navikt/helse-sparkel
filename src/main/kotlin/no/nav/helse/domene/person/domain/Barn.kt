package no.nav.helse.domene.person.domain

import no.nav.helse.domene.AktørId
import java.time.LocalDate

data class Barn(
        val id: AktørId,
        val fornavn: String,
        val mellomnavn: String? = null,
        val etternavn: String,
        val fdato: LocalDate,
        val kjønn: Kjønn,
        val statsborgerskap: String,
        val status: String,
        val diskresjonskode: String? = null
)