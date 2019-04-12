package no.nav.helse.ws.arbeidsforhold.domain

import no.nav.helse.ws.organisasjon.domain.Organisasjonsnummer

sealed class Arbeidsgiver {
    data class Virksomhet(val virksomhetsnummer: Organisasjonsnummer): Arbeidsgiver()
    data class Person(val personnummer: String): Arbeidsgiver()
}
