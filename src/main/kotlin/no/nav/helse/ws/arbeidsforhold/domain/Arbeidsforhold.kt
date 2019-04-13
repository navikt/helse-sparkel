package no.nav.helse.ws.arbeidsforhold.domain

import java.time.LocalDate

data class Arbeidsforhold(val arbeidsgiver: Arbeidsgiver,
                          val startdato: LocalDate,
                          val sluttdato: LocalDate? = null,
                          val arbeidsforholdId: Long,
                          val permisjon: List<Permisjon> = emptyList(),
                          val arbeidsavtaler: List<Arbeidsavtale> = emptyList())
