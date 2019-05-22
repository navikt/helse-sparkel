package no.nav.helse.domene.ytelse.domain

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningsgrunnlagTest {

    @Test
    fun `hører sammen med sak når tema og dato matcher`() {
        val identdato = LocalDate.now()

        assertTrue(Beregningsgrunnlag.Sykepenger(
                identdato = identdato,
                periodeFom = null,
                periodeTom = null,
                behandlingstema = Behandlingstema.SykepengerUtenlandsopphold,
                vedtak = emptyList()
        ).hørerSammenMed(InfotrygdSak(
                sakId = "1",
                iverksatt = identdato,
                tema = Tema.Sykepenger,
                behandlingstema = Behandlingstema.SykepengerUtenlandsopphold,
                opphørerFom = null
        )))
    }

    @Test
    fun `hører ikke sammen med sak når dato er ulik`() {
        val identdato = LocalDate.now()

        assertFalse(Beregningsgrunnlag.Sykepenger(
                identdato = identdato,
                periodeFom = null,
                periodeTom = null,
                behandlingstema = Behandlingstema.SykepengerUtenlandsopphold,
                vedtak = emptyList()
        ).hørerSammenMed(InfotrygdSak(
                sakId = "1",
                iverksatt = identdato.minusDays(1),
                tema = Tema.Sykepenger,
                behandlingstema = Behandlingstema.SykepengerUtenlandsopphold,
                opphørerFom = null
        )))
    }

    @Test
    fun `hører ikke sammen med sak når tema og dato er ulike`() {
        val identdato = LocalDate.now()

        assertFalse(Beregningsgrunnlag.Sykepenger(
                identdato = identdato,
                periodeFom = null,
                periodeTom = null,
                behandlingstema = Behandlingstema.SykepengerUtenlandsopphold,
                vedtak = emptyList()
        ).hørerSammenMed(InfotrygdSak(
                sakId = "1",
                iverksatt = identdato,
                tema = Tema.Foreldrepenger,
                behandlingstema = Behandlingstema.EngangstønadMedFødsel,
                opphørerFom = null
        )))
    }
}
