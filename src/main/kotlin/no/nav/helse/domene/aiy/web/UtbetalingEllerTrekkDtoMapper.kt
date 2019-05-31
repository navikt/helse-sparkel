package no.nav.helse.domene.aiy.web

import no.nav.helse.domene.aiy.domain.UtbetalingEllerTrekk
import no.nav.helse.domene.aiy.domain.Virksomhet
import no.nav.helse.domene.aiy.web.dto.UtbetalingEllerTrekkDTO
import no.nav.helse.domene.aiy.web.dto.VirksomhetDTO

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
