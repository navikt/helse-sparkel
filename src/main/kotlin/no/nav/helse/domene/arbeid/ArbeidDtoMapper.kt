package no.nav.helse.domene.arbeid

import no.nav.helse.domene.arbeid.domain.Arbeidsavtale
import no.nav.helse.domene.arbeid.domain.Arbeidsforhold
import no.nav.helse.domene.arbeid.domain.Permisjon
import no.nav.helse.domene.arbeid.dto.ArbeidsavtaleDTO
import no.nav.helse.domene.arbeid.dto.ArbeidsforholdDTO
import no.nav.helse.domene.arbeid.dto.ArbeidsgiverDTO
import no.nav.helse.domene.arbeid.dto.PermisjonDTO
import no.nav.helse.domene.utbetaling.domain.Virksomhet

object ArbeidDtoMapper {

    fun toDto(arbeidsforhold: Arbeidsforhold) = ArbeidsforholdDTO(
            type = arbeidsforhold.type(),
            arbeidsgiver = toArbeidsgiver(arbeidsforhold.arbeidsgiver),
            startdato = arbeidsforhold.startdato,
            sluttdato = arbeidsforhold.sluttdato,
            yrke = when (arbeidsforhold) {
                is Arbeidsforhold.Frilans -> arbeidsforhold.yrke
                is Arbeidsforhold.Arbeidstaker -> arbeidsforhold.arbeidsavtaler.firstOrNull()?.yrke
            },
            arbeidsavtaler = when (arbeidsforhold) {
                is Arbeidsforhold.Arbeidstaker -> toArbeidsavtalerDto(arbeidsforhold.arbeidsavtaler)
                else -> emptyList()
            },
            permisjon = when (arbeidsforhold) {
                is Arbeidsforhold.Arbeidstaker -> toPermisjonDto(arbeidsforhold.permisjon)
                else -> emptyList()
            }
    )

    fun toArbeidsgiver(virksomhet: Virksomhet) = ArbeidsgiverDTO(virksomhet.identifikator, virksomhet.type())

    fun toArbeidsavtalerDto(arbeidsavtaler: List<Arbeidsavtale>) =
            arbeidsavtaler.map { arbeidsavtale ->
                ArbeidsavtaleDTO(
                        yrke = arbeidsavtale.yrke,
                        stillingsprosent = arbeidsavtale.stillingsprosent,
                        fom = arbeidsavtale.fom,
                        tom = arbeidsavtale.tom
                )
            }

    fun toPermisjonDto(permisjonsliste: List<Permisjon>) =
            permisjonsliste.map { permisjon ->
                PermisjonDTO(
                        fom = permisjon.fom,
                        tom = permisjon.tom,
                        permisjonsprosent = permisjon.permisjonsprosent,
                        arsak = permisjon.årsak
                )
            }
}
