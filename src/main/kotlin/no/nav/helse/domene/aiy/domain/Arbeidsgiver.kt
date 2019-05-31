package no.nav.helse.domene.aiy.domain

import no.nav.helse.domene.aiy.organisasjon.domain.Organisasjonsnummer

sealed class Arbeidsgiver {
    data class Virksomhet(val virksomhetsnummer: Organisasjonsnummer): Arbeidsgiver()
    data class Person(val personnummer: String): Arbeidsgiver()
}
