package no.nav.helse.domene.ytelse

import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.ytelse.domain.Kilde
import no.nav.helse.domene.ytelse.domain.Ytelse
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.informasjon.Grunnlag
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Sak
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Vedtak

object YtelseMapper {

    fun fraInfotrygd(grunnlag: Grunnlag) =
            Ytelse(
                    kilde = Kilde.Infotrygd,
                    tema = grunnlag.behandlingstema.value,
                    fom = grunnlag.periode.fom?.toLocalDate(),
                    tom = grunnlag.periode.tom?.toLocalDate()
            )

    fun fraArena(sak: Sak, vedtak: Vedtak) =
            Ytelse(
                    kilde = Kilde.Arena,
                    tema = sak.tema.value,
                    fom = vedtak.vedtaksperiode.fom.toLocalDate(),
                    tom = vedtak.vedtaksperiode.tom.toLocalDate()
            )
}
