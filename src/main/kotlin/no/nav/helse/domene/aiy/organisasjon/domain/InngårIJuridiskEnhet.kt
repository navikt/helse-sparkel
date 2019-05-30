package no.nav.helse.domene.aiy.organisasjon.domain

import java.time.LocalDate

data class InngårIJuridiskEnhet(val organisasjonsnummer: Organisasjonsnummer, val fom: LocalDate, val tom: LocalDate?)
