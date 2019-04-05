package no.nav.helse.ws.arbeidsforhold

import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsforhold
import no.nav.helse.ws.arbeidsforhold.domain.Arbeidsgiver
import no.nav.helse.ws.arbeidsforhold.dto.ArbeidsforholdDTO
import no.nav.helse.ws.arbeidsforhold.dto.ArbeidsgiverDTO

object ArbeidDtoMapper {

    fun toDto(arbeidsforhold: Arbeidsforhold) =
            ArbeidsforholdDTO(
                    arbeidsgiver = arbeidsgiver(arbeidsforhold.arbeidsgiver),
                    startdato = arbeidsforhold.startdato,
                    sluttdato = arbeidsforhold.sluttdato)

    fun toDto(it: Arbeidsgiver.Virksomhet) =
            ArbeidsgiverDTO(it.virksomhet.orgnr.value, it.virksomhet.navn)

    private fun arbeidsgiver(arbeidsgiver: Arbeidsgiver) = when (arbeidsgiver) {
        is Arbeidsgiver.Virksomhet -> ArbeidsgiverDTO(arbeidsgiver.virksomhet.orgnr.value, arbeidsgiver.virksomhet.navn)
        is Arbeidsgiver.Person -> throw NotImplementedError("arbeidsgiver of type Person is not implemented")
    }
}
