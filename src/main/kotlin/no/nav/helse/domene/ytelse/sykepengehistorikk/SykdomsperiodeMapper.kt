package no.nav.helse.domene.ytelse.sykepengehistorikk

import no.nav.helse.oppslag.spole.Periode

object SykdomsperiodeMapper {

    fun toDto(periode: Periode) =
            PeriodeDto(
                    fom = periode.fom,
                    tom = periode.tom,
                    grad = periode.grad
            )
}

