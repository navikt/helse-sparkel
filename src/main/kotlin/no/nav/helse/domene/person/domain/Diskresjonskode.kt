package no.nav.helse.domene.person.domain

data class Diskresjonskode(
    val forkortelse : String,
    val beskrivelse : String? = null,
    val kode : Int? = null
)
