package no.nav.helse.ws.aiy

import no.nav.helse.ws.aiy.domain.ArbeidsforholdMedInntekt
import no.nav.helse.ws.aiy.dto.ArbeidsforholdMedInntektDTO
import no.nav.helse.ws.arbeidsforhold.ArbeidDtoMapper

object ArbeidsInntektYtelseDtoMapper {

    fun toDto(arbeidsforholdMedInntekt: ArbeidsforholdMedInntekt) =
            ArbeidsforholdMedInntektDTO(
                    arbeidsforhold = ArbeidDtoMapper.toDto(arbeidsforholdMedInntekt.arbeidsforhold),
                    inntekter = arbeidsforholdMedInntekt.inntekter)

}
