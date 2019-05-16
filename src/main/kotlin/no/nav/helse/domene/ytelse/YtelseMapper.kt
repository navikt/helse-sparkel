package no.nav.helse.domene.ytelse

import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.ytelse.domain.Kilde
import no.nav.helse.domene.ytelse.domain.Ytelse
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Vedtak
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Sak

object YtelseMapper {

    fun fraInfotrygd(tema: String, vedtak: Vedtak) =
            Ytelse(
                    kilde = Kilde.Infotrygd,
                    tema = tema,
                    fom = vedtak.anvistPeriode.fom.toLocalDate(),
                    tom = vedtak.anvistPeriode.tom.toLocalDate()
            )

    fun fraArena(sak: Sak, vedtak: no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Vedtak) =
            Ytelse(
                    kilde = Kilde.Arena,
                    tema = sak.tema.value,
                    fom = vedtak.vedtaksperiode.fom.toLocalDate(),
                    tom = vedtak.vedtaksperiode.tom.toLocalDate()
            )
}
