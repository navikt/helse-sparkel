package no.nav.helse.domene.aiy

import arrow.core.flatMap
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.aiy.aareg.ArbeidstakerService
import no.nav.helse.domene.aiy.inntektskomponenten.FrilansArbeidsforholdService
import java.time.LocalDate
import java.time.YearMonth

class ArbeidsforholdService(private val arbeidstakerService: ArbeidstakerService,
                            private val frilansArbeidsforholdService: FrilansArbeidsforholdService) {

    fun finnArbeidsforhold(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            frilansArbeidsforholdService.hentFrilansarbeidsforhold(aktørId, YearMonth.from(fom), YearMonth.from(tom)).flatMap { frilansArbeidsforholdliste ->
                arbeidstakerService.finnArbeidstakerarbeidsforhold(aktørId, fom, tom).map { arbeidsforholdliste ->
                    arbeidsforholdliste.plus(frilansArbeidsforholdliste)
                }
            }
}
