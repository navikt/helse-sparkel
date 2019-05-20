package no.nav.helse.domene.ytelse

import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.ytelse.InfotrygdSakMapper.toSak
import no.nav.helse.domene.ytelse.domain.Behandlingstema
import no.nav.helse.domene.ytelse.domain.InfotrygdSak
import no.nav.helse.domene.ytelse.domain.Tema
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
            this.iverksatt = iverksatt.toXmlGregorianCalendar()
            this.tema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Tema().apply {
                value = tema
            }
            this.behandlingstema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Behandlingstema().apply {
                value = behandlingstema
            }
        }

        val expected = InfotrygdSak(
                iverksatt = iverksatt,
                tema = Tema.Sykepenger,
                behandlingstema = Behandlingstema.SykepengerUtenlandsopphold,
                opphørerFom = null
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
            this.iverksatt = iverksatt.toXmlGregorianCalendar()
            this.tema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Tema().apply {
                value = tema
            }
            this.behandlingstema = no.nav.tjeneste.virksomhet.infotrygdsak.v1.informasjon.Behandlingstema().apply {
                value = behandlingstema
            }
            this.opphoerFom = opphørerFom.toXmlGregorianCalendar()
        }

        val expected = InfotrygdSak(
                iverksatt = iverksatt,
                tema = Tema.Sykepenger,
                behandlingstema = Behandlingstema.SykepengerUtenlandsopphold,
                opphørerFom = opphørerFom
        )

        assertEquals(expected, toSak(given))
    }
}
