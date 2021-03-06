package no.nav.helse.domene.ytelse.infotrygd

import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.ytelse.domain.Behandlingstema
import no.nav.helse.domene.ytelse.domain.InfotrygdSak
import no.nav.helse.domene.ytelse.domain.Tema
import no.nav.helse.domene.ytelse.infotrygd.InfotrygdSakMapper.toSak
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InfotrygdSakMapperTest {

    @Test
    fun `skal mappe infotrygdsak`() {
        val iverksatt = LocalDate.now()
        val tema = "SP"
        val behandlingstema = "SU"

        val given = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.InfotrygdSak().apply {
            this.sakId = "1"
            this.iverksatt = iverksatt.toXmlGregorianCalendar()
            this.tema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Tema().apply {
                value = tema
            }
            this.behandlingstema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Behandlingstema().apply {
                value = behandlingstema
            }
        }

        val expected = InfotrygdSak.Sak(
                tema = Tema.Sykepenger,
                behandlingstema = Behandlingstema.SykepengerUtenlandsopphold
        )

        assertEquals(expected, toSak(given))
    }

    @Test
    fun `skal mappe infotrygdvedtak`() {
        val iverksatt = LocalDate.now()
        val tema = "SP"
        val behandlingstema = "SU"
        val opphørerFom = LocalDate.now()

        val given = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.InfotrygdVedtak().apply {
            this.sakId = "1"
            this.iverksatt = iverksatt.toXmlGregorianCalendar()
            this.tema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Tema().apply {
                value = tema
            }
            this.behandlingstema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Behandlingstema().apply {
                value = behandlingstema
            }
            this.opphoerFom = opphørerFom.toXmlGregorianCalendar()
            this.status = Status().apply {
                value = "A"
            }
        }

        val expected = InfotrygdSak.Vedtak(
                iverksatt = iverksatt,
                tema = Tema.Sykepenger,
                behandlingstema = Behandlingstema.SykepengerUtenlandsopphold,
                opphørerFom = opphørerFom,
                ikkeStartet = false
        )

        assertEquals(expected, toSak(given))
    }
}
