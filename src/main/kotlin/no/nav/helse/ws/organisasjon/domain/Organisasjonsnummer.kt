package no.nav.helse.ws.organisasjon.domain

import no.nav.helse.ws.organisasjon.Organisasjonsnummervalidator

data class Organisasjonsnummer(val value : String) {
    init {
        if (!Organisasjonsnummervalidator.erGyldig(value)) {
            throw IllegalArgumentException("Organisasjonsnummer $value er ugyldig")
        }
    }
}
