package no.nav.helse.ws

data class Fødselsnummer(val value: String) {
    val elevenDigits = Regex("\\d{11}")

    init {
        if (!elevenDigits.matches(value)) {
            throw IllegalArgumentException("$value is not a valid fnr")
        }
    }
}