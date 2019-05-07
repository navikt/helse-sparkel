package no.nav.helse.domene.aiy.domain

import no.nav.helse.domene.arbeid.domain.Arbeidsforhold
import no.nav.helse.domene.utbetaling.domain.UtbetalingEllerTrekk

data class ArbeidInntektYtelse(val lønnsinntekter: List<Pair<UtbetalingEllerTrekk.Lønn, List<Arbeidsforhold>>> = emptyList(),
                               val arbeidsforhold: List<Arbeidsforhold> = emptyList(),
                               val ytelser: List<UtbetalingEllerTrekk.Ytelse> = emptyList(),
                               val pensjonEllerTrygd: List<UtbetalingEllerTrekk.PensjonEllerTrygd> = emptyList(),
                               val næringsinntekt: List<UtbetalingEllerTrekk.Næring> = emptyList())
