package no.nav.helse.domene.ytelse

import arrow.core.flatMap
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.ytelse.arena.ArenaService
import no.nav.helse.domene.ytelse.domain.Ytelser
import no.nav.helse.domene.ytelse.infotrygd.InfotrygdService
import java.time.LocalDate

class YtelseService(private val aktørregisterService: AktørregisterService,
                    private val infotrygdService: InfotrygdService,
                    private val arenaService: ArenaService) {

    fun finnYtelser(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            arenaService.finnSaker(aktørId, fom, tom).flatMap { ytelserFraArena ->
                aktørregisterService.fødselsnummerForAktør(aktørId).flatMap { fnr ->
                    infotrygdService.finnSakerOgGrunnlag(Fødselsnummer(fnr), fom, tom)
                }.map { ytelserFraInfotrygd ->
                    Ytelser(arena = ytelserFraArena, infotrygd = ytelserFraInfotrygd)
                }
            }
}
