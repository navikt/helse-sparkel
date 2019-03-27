package no.nav.helse.ws.arbeidsforhold.domain

import no.nav.helse.ws.organisasjon.domain.Organisasjon

sealed class Arbeidsgiver {
    data class Virksomhet(val virksomhet: Organisasjon.Virksomhet): Arbeidsgiver()
    data class Person(val personnummer: String): Arbeidsgiver()
}
