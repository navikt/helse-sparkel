package no.nav.helse.ws

data class AktørId(val aktor: String) {

    init {
        if (aktor.isEmpty()) {
            throw IllegalArgumentException("$aktor cannot be empty")
        }
    }

}