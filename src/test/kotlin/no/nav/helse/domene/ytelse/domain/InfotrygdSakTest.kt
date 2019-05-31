package no.nav.helse.domene.ytelse.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InfotrygdSakTest {
    @Test
    fun `infotrygd vedtak er ulik en infotrygd sak`() {
        val vedtak = etInfotrygdVedtak()
        val sak = enInfotrygdSak()

        assertNotEquals(vedtak, sak)
        assertNotEquals(vedtak.hashCode(), sak.hashCode())
    }

    @Test
    fun `infotrygd vedtak er lik en annen med samme verdier`() {
        val vedtak1 = etInfotrygdVedtak()
        val vedtak2 = etInfotrygdVedtak()

        assertEquals(vedtak1, vedtak2)
        assertEquals(vedtak1.hashCode(), vedtak2.hashCode())
    }

    @Test
    fun `infotrygd vedtak er ulik en annen med ulike verdier`() {
        val vedtak1 = etInfotrygdVedtak()
        val vedtak2 = etInfotrygdVedtak()
                .medTema(Tema.Foreldrepenger)

        assertNotEquals(vedtak1, vedtak2)
        assertNotEquals(vedtak1.hashCode(), vedtak2.hashCode())

        val vedtak3 = etInfotrygdVedtak()
                .medBehandlingstema(Behandlingstema.Foreldrepenger)

        assertNotEquals(vedtak1, vedtak3)
        assertNotEquals(vedtak1.hashCode(), vedtak3.hashCode())

        val vedtak4 = etInfotrygdVedtak()
                .medIverksatt(LocalDate.now().minusDays(1))

        assertNotEquals(vedtak1, vedtak4)
        assertNotEquals(vedtak1.hashCode(), vedtak4.hashCode())

        val vedtak5 = etInfotrygdVedtak()
                .medOpphørerFom(LocalDate.now().minusDays(1))

        assertNotEquals(vedtak1, vedtak5)
        assertNotEquals(vedtak1.hashCode(), vedtak5.hashCode())
    }

    @Test
    fun `infotrygd sak er lik en annen med samme verdier`() {
        val sak1 = enInfotrygdSak()
        val sak2 = enInfotrygdSak()

        assertEquals(sak1, sak2)
        assertEquals(sak1.hashCode(), sak2.hashCode())
    }

    @Test
    fun `infotrygd sak er ulik en annen med ulike verdier`() {
        val sak1 = enInfotrygdSak()
        val sak2 = enInfotrygdSak()
                .medTema(Tema.Foreldrepenger)

        assertNotEquals(sak1, sak2)
        assertNotEquals(sak1.hashCode(), sak2.hashCode())

        val sak3 = enInfotrygdSak()
                .medBehandlingstema(Behandlingstema.Foreldrepenger)

        assertNotEquals(sak1, sak3)
        assertNotEquals(sak1.hashCode(), sak3.hashCode())

        val sak4 = enInfotrygdSak()
                .medIverksatt(LocalDate.now().minusDays(1))

        assertNotEquals(sak1, sak4)
        assertNotEquals(sak1.hashCode(), sak4.hashCode())
    }

    @Test
    fun `test string-representasjon av infotrygd vedtak`() {
        val vedtak = etInfotrygdVedtak()
                .medIverksatt(LocalDate.of(2019, 1, 1))
                .medOpphørerFom(LocalDate.of(2020, 1, 1))
        assertEquals("InfotrygdSak.Vedtak(sakId=null, tema=Sykepenger, behandlingstema=Sykepenger(kode='SP', tema=Sykepenger), iverksatt=2019-01-01, opphørerFom=2020-01-01)", vedtak.toString())
    }

    @Test
    fun `test string-representasjon av infotrygd sak`() {
        val sak = enInfotrygdSak()
                .medIverksatt(LocalDate.of(2019, 1, 1))
        assertEquals("InfotrygdSak.Åpen(sakId=null, tema=Sykepenger, behandlingstema=Sykepenger(kode='SP', tema=Sykepenger), iverksatt=2019-01-01)", sak.toString())
    }

    private fun etInfotrygdVedtak() =
            InfotrygdSak.Vedtak(
                    sakId = null,
                    tema = Tema.Sykepenger,
                    behandlingstema = Behandlingstema.Sykepenger,
                    iverksatt = LocalDate.now(),
                    opphørerFom = LocalDate.now().plusMonths(1)
            )

    private fun enInfotrygdSak() =
            InfotrygdSak.Åpen(
                    sakId = null,
                    tema = Tema.Sykepenger,
                    behandlingstema = Behandlingstema.Sykepenger,
                    iverksatt = LocalDate.now()
            )

    private fun InfotrygdSak.Vedtak.medTema(tema: Tema) =
            InfotrygdSak.Vedtak(
                    sakId = sakId,
                    tema = tema,
                    behandlingstema = behandlingstema,
                    iverksatt = iverksatt,
                    opphørerFom = opphørerFom
            )

    private fun InfotrygdSak.Vedtak.medBehandlingstema(behandlingstema: Behandlingstema) =
            InfotrygdSak.Vedtak(
                    sakId = sakId,
                    tema = tema,
                    behandlingstema = behandlingstema,
                    iverksatt = iverksatt,
                    opphørerFom = opphørerFom
            )

    private fun InfotrygdSak.Vedtak.medIverksatt(iverksatt: LocalDate) =
            InfotrygdSak.Vedtak(
                    sakId = sakId,
                    tema = tema,
                    behandlingstema = behandlingstema,
                    iverksatt = iverksatt,
                    opphørerFom = opphørerFom
            )

    private fun InfotrygdSak.Vedtak.medOpphørerFom(opphørerFom: LocalDate) =
            InfotrygdSak.Vedtak(
                    sakId = sakId,
                    tema = tema,
                    behandlingstema = behandlingstema,
                    iverksatt = iverksatt,
                    opphørerFom = opphørerFom
            )

    private fun InfotrygdSak.Åpen.medTema(tema: Tema) =
            InfotrygdSak.Åpen(
                    sakId = sakId,
                    tema = tema,
                    behandlingstema = behandlingstema,
                    iverksatt = iverksatt
            )

    private fun InfotrygdSak.Åpen.medBehandlingstema(behandlingstema: Behandlingstema) =
            InfotrygdSak.Åpen(
                    sakId = sakId,
                    tema = tema,
                    behandlingstema = behandlingstema,
                    iverksatt = iverksatt
            )

    private fun InfotrygdSak.Åpen.medIverksatt(iverksatt: LocalDate) =
            InfotrygdSak.Åpen(
                    sakId = sakId,
                    tema = tema,
                    behandlingstema = behandlingstema,
                    iverksatt = iverksatt
            )
}
