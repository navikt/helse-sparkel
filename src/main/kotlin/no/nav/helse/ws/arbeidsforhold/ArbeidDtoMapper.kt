package no.nav.helse.ws.arbeidsforhold

import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsavtale
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsgiver
import no.nav.helse.ws.arbeidsforhold.domain.Permisjon
import no.nav.helse.ws.arbeidsforhold.dto.ArbeidsavtaleDTO
import no.nav.helse.ws.arbeidsforhold.dto.ArbeidsforholdDTO
import no.nav.helse.ws.arbeidsforhold.dto.ArbeidsgiverDTO
import no.nav.helse.ws.arbeidsforhold.dto.PermisjonDTO

object ArbeidDtoMapper {

    fun toDto(arbeidsforhold: Arbeidsforhold) =
            ArbeidsforholdDTO(
                    arbeidsgiver = arbeidsgiver(arbeidsforhold.arbeidsgiver),
                    startdato = arbeidsforhold.startdato,
                    sluttdato = arbeidsforhold.sluttdato,
                    arbeidsavtaler = toArbeidsavtalerDto(arbeidsforhold.arbeidsavtaler),
                    permisjon = toPermisjonDto(arbeidsforhold.permisjon)
            )

    private fun arbeidsgiver(arbeidsgiver: Arbeidsgiver) = when (arbeidsgiver) {
        is Arbeidsgiver.Virksomhet -> ArbeidsgiverDTO(arbeidsgiver.virksomhetsnummer.value, null)
        is Arbeidsgiver.Person -> throw NotImplementedError("arbeidsgiver of type Person is not implemented")
    }

    fun toArbeidsavtalerDto(arbeidsavtaler: List<Arbeidsavtale>) =
            arbeidsavtaler.map { arbeidsavtale ->
                ArbeidsavtaleDTO(
                        yrke = arbeidsavtale.yrke,
                        stillingsprosent = arbeidsavtale.stillingsprosent,
                        fom = arbeidsavtale.fom,
                        tom = arbeidsavtale.tom
                )
            }

    fun toPermisjonDto(permisjon: List<Permisjon>) =
            permisjon.map { permisjon ->
                PermisjonDTO(
                        fom = permisjon.fom,
                        tom = permisjon.tom,
                        permisjonsprosent = permisjon.permisjonsprosent,
                        arsak = permisjon.årsak
                )
            }
}
