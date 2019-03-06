package no.nav.helse.ws.meldekort

import no.nav.helse.Feilårsak
import no.nav.helse.bimap
import no.nav.helse.common.toLocalDate
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeAktoerIkkeFunnet
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.FinnMeldekortUtbetalingsgrunnlagListeUgyldigInput
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Meldekort
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Sak
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Vedtak
import java.time.LocalDate

class MeldekortService(private val meldekortClient: MeldekortClient) {

    fun hentMeldekortgrunnlag(aktørId: AktørId, fom: LocalDate, tom: LocalDate) =
            meldekortClient.hentMeldekortgrunnlag(aktørId.aktor, fom, tom).bimap({
                when (it) {
                    is FinnMeldekortUtbetalingsgrunnlagListeAktoerIkkeFunnet -> Feilårsak.IkkeFunnet
                    is FinnMeldekortUtbetalingsgrunnlagListeSikkerhetsbegrensning -> Feilårsak.FeilFraTjeneste
                    is FinnMeldekortUtbetalingsgrunnlagListeUgyldigInput -> Feilårsak.FeilFraTjeneste
                    else -> Feilårsak.FeilFraTjeneste
                }
            }, {
                it.flatMap(::toSak)
            })

    private fun toSak(sak: Sak): List<MeldekortUtbetalingsgrunnlagSak> {
        return when {
            sak.vedtakListe.isEmpty() -> sakUtenVedtak(sak)
            else -> sakMedVedtakListe(sak)
        }
    }

    private fun sakMedVedtakListe(sak: Sak): List<MeldekortUtbetalingsgrunnlagSak> {
        return sak.vedtakListe.map { vedtak -> sakMedVedtak(sak, vedtak) }
    }

    private fun sakMedVedtak(sak: Sak, vedtak: Vedtak): MeldekortUtbetalingsgrunnlagSak {
        return MeldekortUtbetalingsgrunnlagSak(
                type = sak.tema.termnavn,
                kilde = "ARENA",
                saksnummer = sak.fagsystemSakId,
                saksstatus = sak.saksstatus.termnavn,
                kravMottattDato = vedtak.datoKravMottatt.toLocalDate(),
                vedtaksstatus = vedtak.vedtaksstatus.termnavn,
                vedtakFom = vedtak.vedtaksperiode.fom.toLocalDate(),
                vedtakTom = vedtak.vedtaksperiode.tom.toLocalDate(),
                meldekort = vedtak.meldekortListe.map(this::toMeldekort)
        )
    }

    private fun toMeldekort(meldekort: Meldekort): MeldekortForVedtak {
        return MeldekortForVedtak(
                fom = meldekort.meldekortperiode.fom.toLocalDate(),
                tom = meldekort.meldekortperiode.tom.toLocalDate(),
                belop = meldekort.beloep,
                dagsats = meldekort.dagsats,
                utbetalingsgrad = meldekort.utbetalingsgrad
        )
    }

    private fun sakUtenVedtak(sak: Sak): List<MeldekortUtbetalingsgrunnlagSak> {
        return listOf(MeldekortUtbetalingsgrunnlagSak(
                type = sak.tema.termnavn,
                kilde = "ARENA",
                saksnummer = sak.fagsystemSakId,
                saksstatus = sak.saksstatus.termnavn
        ))
    }
}

data class MeldekortUtbetalingsgrunnlagSak(
        val type: String,
        val kilde: String,
        val saksnummer: String,
        val saksstatus: String,
        val kravMottattDato: LocalDate? = null,
        val vedtaksstatus: String? = null,
        val meldekort: List<MeldekortForVedtak> = emptyList(),
        val vedtakFom: LocalDate? = null,
        val vedtakTom: LocalDate? = null
)

data class MeldekortForVedtak(
        val fom: LocalDate,
        val tom: LocalDate,
        val belop: Double,
        val dagsats: Double,
        val utbetalingsgrad: Double
)
