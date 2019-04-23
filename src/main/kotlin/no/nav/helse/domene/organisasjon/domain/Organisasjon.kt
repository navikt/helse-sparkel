package no.nav.helse.domene.organisasjon.domain

sealed class Organisasjon(open val orgnr: Organisasjonsnummer, open val navn: String?) {
    data class JuridiskEnhet(override val orgnr: Organisasjonsnummer, override val navn: String? = null): Organisasjon(orgnr, navn)
    data class Organisasjonsledd(override val orgnr: Organisasjonsnummer, override val navn: String? = null): Organisasjon(orgnr, navn)
    data class Virksomhet(override val orgnr: Organisasjonsnummer, override val navn: String? = null, val inngårIJuridiskEnhet: List<InngårIJuridiskEnhet> = emptyList()): Organisasjon(orgnr, navn)

    fun type() = when (this) {
        is JuridiskEnhet -> "JuridiskEnhet"
        is Organisasjonsledd -> "Orgledd"
        is Virksomhet -> "Virksomhet"
    }
}
