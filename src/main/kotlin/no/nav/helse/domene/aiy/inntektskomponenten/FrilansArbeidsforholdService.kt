package no.nav.helse.domene.aiy.inntektskomponenten

import no.nav.helse.domene.AktørId
import no.nav.helse.domene.aiy.ArbeidDomainMapper
import no.nav.helse.oppslag.inntekt.InntektClient
import no.nav.helse.probe.DatakvalitetProbe
import java.time.YearMonth

class FrilansArbeidsforholdService(private val inntektClient: InntektClient,
                                   private val datakvalitetProbe: DatakvalitetProbe) {

    fun hentFrilansarbeidsforhold(aktørId: AktørId, fom: YearMonth, tom: YearMonth) =
            inntektClient.hentFrilansArbeidsforhold(aktørId, fom, tom)
                    .toEither(InntektskomponentenErrorMapper::mapToError)
                    .map {
                        it.map(ArbeidDomainMapper::toArbeidsforhold)
                                .filterNotNull()
                                .also {
                                    datakvalitetProbe.frilansArbeidsforhold(it)
                                }
                    }.map { arbeidsforholdliste ->
                        arbeidsforholdliste.onEach { arbeidsforhold ->
                            datakvalitetProbe.inspiserFrilans(arbeidsforhold)
                        }
                    }
}
