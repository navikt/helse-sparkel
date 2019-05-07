package no.nav.helse.domene.utbetaling

import no.nav.helse.domene.utbetaling.domain.UtbetalingEllerTrekk
import no.nav.helse.domene.utbetaling.domain.Virksomhet
import no.nav.helse.domene.utbetaling.dto.UtbetalingEllerTrekkDTO
import no.nav.helse.domene.utbetaling.dto.VirksomhetDTO

object UtbetalingEllerTrekkDtoMapper {

    fun toDto(virksomhet: Virksomhet) = VirksomhetDTO(virksomhet.identifikator, virksomhet.type())

    fun toDto(utbetalingEllerTrekk: UtbetalingEllerTrekk) =
            UtbetalingEllerTrekkDTO(
                    virksomhet = toDto(utbetalingEllerTrekk.virksomhet),
                    utbetalingsperiode = utbetalingEllerTrekk.utbetalingsperiode,
                    beløp = utbetalingEllerTrekk.beløp,
                    type = utbetalingEllerTrekk.type(),
                    ytelse = utbetalingEllerTrekk.isYtelse(),
                    kode = utbetalingEllerTrekk.kode())
}
