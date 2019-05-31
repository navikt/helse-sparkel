package no.nav.helse.domene.aiy.web

import no.nav.helse.domene.aiy.domain.Arbeidsavtale
import no.nav.helse.domene.aiy.domain.Arbeidsforhold
import no.nav.helse.domene.aiy.domain.Permisjon
import no.nav.helse.domene.aiy.domain.Virksomhet
import no.nav.helse.domene.aiy.web.dto.ArbeidsavtaleDTO
import no.nav.helse.domene.aiy.web.dto.ArbeidsforholdDTO
import no.nav.helse.domene.aiy.web.dto.ArbeidsgiverDTO
import no.nav.helse.domene.aiy.web.dto.PermisjonDTO

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
                        tom = if (arbeidsavtale is Arbeidsavtale.Historisk) arbeidsavtale.tom else null
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
