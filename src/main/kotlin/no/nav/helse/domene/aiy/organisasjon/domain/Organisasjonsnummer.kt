package no.nav.helse.domene.aiy.organisasjon.domain

import no.nav.helse.domene.aiy.organisasjon.Organisasjonsnummervalidator

data class Organisasjonsnummer(val value : String) {
    init {
        if (!Organisasjonsnummervalidator.erGyldig(value)) {
            throw IllegalArgumentException("Organisasjonsnummer $value er ugyldig")
        }
    }
}
