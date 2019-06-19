package no.nav.helse.domene.ytelse

import arrow.core.Either
import arrow.core.right
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.Fødselsnummer
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.ytelse.arena.ArenaService
import no.nav.helse.domene.ytelse.domain.*
import no.nav.helse.domene.ytelse.infotrygd.InfotrygdService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class YtelseServiceTest {

    @Test
    fun `skal sammenstille ytelser fra arena og infotrygd`() {
        val aktørregisterService = mockk<AktørregisterService>()
        val infotrygdService = mockk<InfotrygdService>()
        val arenaService = mockk<ArenaService>()

        val ytelseService = YtelseService(
                aktørregisterService = aktørregisterService,
                infotrygdService = infotrygdService,
                arenaService = arenaService
        )

        val aktørId = AktørId("123456789")
        val fødselsnummer = Fødselsnummer("11111111111")
        val identdatoSykepenger = LocalDate.now().minusMonths(1)
        val identdatoForeldrepenger = LocalDate.now().minusDays(28)
        val identdatoEngangstønad = LocalDate.now().minusDays(14)
        val identdatoPleiepenger = LocalDate.now().minusDays(7)
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now()

        every {
            aktørregisterService.fødselsnummerForAktør(aktørId)
        } returns fødselsnummer.value.right()

        every {
            infotrygdService.finnSakerOgGrunnlag(fødselsnummer, fom, tom)
        } returns sakerFraInfotrygd(fom, tom, identdatoSykepenger, identdatoForeldrepenger, identdatoEngangstønad, identdatoPleiepenger).right()

        every {
            arenaService.finnSaker(aktørId, fom, tom)
        } returns ytelserFraArena(fom, tom).right()

        val expectedArena = ytelserFraArena(fom, tom)
        val expectedInfotrygd = sakerFraInfotrygd(fom, tom, identdatoSykepenger, identdatoForeldrepenger, identdatoEngangstønad, identdatoPleiepenger)
        val actual = ytelseService.finnYtelser(aktørId, fom, tom)

        assertTrue(actual is Either.Right)
        actual as Either.Right
        assertEquals(expectedArena, actual.b.arena)
        assertEquals(expectedInfotrygd, actual.b.infotrygd)
    }

    private fun ytelserFraArena(fom: LocalDate, tom: LocalDate) =
            listOf(
                    Ytelse(
                            kilde = Kilde.Arena,
                            tema = "AAP",
                            fom = fom,
                            tom = tom
                    ),
                    Ytelse(
                            kilde = Kilde.Arena,
                            tema = "DAG",
                            fom = fom,
                            tom = tom
                    )
            )

    private fun sakerFraInfotrygd(fom: LocalDate, tom: LocalDate, identdatoSykepenger: LocalDate, identdatoForeldrepenger: LocalDate, identdatoEngangstønad: LocalDate, identdatoPleiepenger: LocalDate) =
            listOf(
                    InfotrygdSakOgGrunnlag(
                            sak = InfotrygdSak.Vedtak(
                                    iverksatt = identdatoSykepenger,
                                    tema = no.nav.helse.domene.ytelse.domain.Tema.Sykepenger,
                                    behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.Sykepenger,
                                    opphørerFom = null,
                                    ikkeStartet = false
                            ),
                            grunnlag = Beregningsgrunnlag.Sykepenger(
                                    identdato = identdatoSykepenger,
                                    periodeFom = fom,
                                    periodeTom = tom,
                                    behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.Sykepenger,
                                    vedtak = emptyList()
                            )
                    ),
                    InfotrygdSakOgGrunnlag(
                            sak = InfotrygdSak.Vedtak(
                                    iverksatt = identdatoForeldrepenger,
                                    tema = no.nav.helse.domene.ytelse.domain.Tema.Foreldrepenger,
                                    behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.ForeldrepengerMedFødsel,
                                    opphørerFom = null,
                                    ikkeStartet = false
                            ),
                            grunnlag = Beregningsgrunnlag.Foreldrepenger(
                                    identdato = identdatoForeldrepenger,
                                    periodeFom = fom,
                                    periodeTom = tom,
                                    behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.ForeldrepengerMedFødsel,
                                    vedtak = emptyList()
                            )
                    ),
                    InfotrygdSakOgGrunnlag(
                            sak = InfotrygdSak.Vedtak(
                                    iverksatt = identdatoEngangstønad,
                                    tema = no.nav.helse.domene.ytelse.domain.Tema.Foreldrepenger,
                                    behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.EngangstønadMedFødsel,
                                    opphørerFom = null,
                                    ikkeStartet = false
                            ),
                            grunnlag = Beregningsgrunnlag.Engangstønad(
                                    identdato = identdatoEngangstønad,
                                    periodeFom = fom,
                                    periodeTom = tom,
                                    behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.EngangstønadMedFødsel,
                                    vedtak = emptyList()
                            )
                    ),
                    InfotrygdSakOgGrunnlag(
                            sak = InfotrygdSak.Vedtak(
                                    iverksatt = identdatoPleiepenger,
                                    tema = no.nav.helse.domene.ytelse.domain.Tema.PårørendeSykdom,
                                    behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.Pleiepenger,
                                    opphørerFom = null,
                                    ikkeStartet = false
                            ),
                            grunnlag = Beregningsgrunnlag.PårørendeSykdom(
                                    identdato = identdatoPleiepenger,
                                    periodeFom = fom,
                                    periodeTom = tom,
                                    behandlingstema = no.nav.helse.domene.ytelse.domain.Behandlingstema.Pleiepenger,
                                    vedtak = emptyList()
                            )
                    )
            )
}
