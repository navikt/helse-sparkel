package no.nav.helse.ws.person.domain

data class Diskresjonskode(
    val forkortelse : String,
    val beskrivelse : String? = null,
    val kode : Int? = null
)
