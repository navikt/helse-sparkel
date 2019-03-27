package no.nav.helse.ws.inntekt.domain

sealed class Arbeidsgiver {
    data class Organisasjon(val orgnr: String): Arbeidsgiver()
}
