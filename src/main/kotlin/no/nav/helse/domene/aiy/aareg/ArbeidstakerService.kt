package no.nav.helse.domene.aiy.aareg

import arrow.core.flatMap
import no.nav.helse.arrow.sequenceU
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.aiy.ArbeidDomainMapper
import no.nav.helse.domene.aiy.domain.Arbeidsforhold
import no.nav.helse.oppslag.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.probe.DatakvalitetProbe
import java.time.LocalDate

class ArbeidstakerService(private val arbeidsforholdClient: ArbeidsforholdClient,
                          private val datakvalitetProbe: DatakvalitetProbe) {

    fun finnArbeidstakerarbeidsforhold(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom)
                    .toEither(AaregErrorMapper::mapToError)
                    .map { liste ->
                        liste.mapNotNull(ArbeidDomainMapper::toArbeidsforhold)
                    }.flatMap { liste ->
                        liste.map { arbeidsforhold ->
                            finnHistoriskeAvtaler(arbeidsforhold).map { avtaler ->
                                arbeidsforhold.copy(
                                        arbeidsavtaler = avtaler
                                )
                            }
                        }.sequenceU()
                    }.map { arbeidsforholdliste ->
                        arbeidsforholdliste.onEach { arbeidsforhold ->
                            datakvalitetProbe.inspiserArbeidstaker(arbeidsforhold)
                        }
                    }

    private fun finnHistoriskeAvtaler(arbeidsforhold: Arbeidsforhold.Arbeidstaker) =
            arbeidsforholdClient.finnHistoriskeArbeidsavtaler(arbeidsforhold.arbeidsforholdId)
                    .toEither(AaregErrorMapper::mapToError)
                    .map { avtaler ->
                        avtaler.map(ArbeidDomainMapper::toArbeidsavtale)
                    }
}
