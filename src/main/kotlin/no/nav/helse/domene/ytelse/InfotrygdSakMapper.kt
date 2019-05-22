package no.nav.helse.domene.ytelse

import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.ytelse.domain.Behandlingstema
import no.nav.helse.domene.ytelse.domain.InfotrygdSak
import no.nav.helse.domene.ytelse.domain.Tema
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.InfotrygdVedtak
import org.slf4j.LoggerFactory

object InfotrygdSakMapper {

    private val log = LoggerFactory.getLogger(InfotrygdSakMapper::class.java)

    fun toSak(sak: no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.InfotrygdSak) =
            InfotrygdSak(
                    sakId = sak.sakId,
                    iverksatt = sak.iverksatt?.toLocalDate(),
                    tema = Tema.fraKode(sak.tema.value),
                    behandlingstema = Behandlingstema.fraKode(sak.behandlingstema.value),
                    opphørerFom = if (sak is InfotrygdVedtak) sak.opphoerFom?.toLocalDate() else null
            ).also {
                log.info("mapper sak med sakId=${sak.sakId} type=${sak.type?.value} status=${sak.status?.value} resultat=${sak.resultat?.value}")
            }
}
