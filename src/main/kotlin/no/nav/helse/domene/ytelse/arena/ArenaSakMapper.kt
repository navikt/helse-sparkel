package no.nav.helse.domene.ytelse.arena

import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.ytelse.domain.Kilde
import no.nav.helse.domene.ytelse.domain.Ytelse
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Sak
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Vedtak

object ArenaSakMapper {

    fun fraArena(sak: Sak, vedtak: Vedtak) =
            Ytelse(
                    kilde = Kilde.Arena,
                    tema = sak.tema.value,
                    fom = vedtak.vedtaksperiode.fom?.toLocalDate(),
                    tom = vedtak.vedtaksperiode.tom?.toLocalDate()
            )
}
