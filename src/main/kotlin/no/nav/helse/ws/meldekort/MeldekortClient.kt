package no.nav.helse.ws.meldekort

import no.nav.helse.Either
import no.nav.helse.common.toLocalDate
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.MeldekortUtbetalingsgrunnlagV1
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Meldekort
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.ObjectFactory
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Periode
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Sak
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.Vedtak
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.meldinger.FinnMeldekortUtbetalingsgrunnlagListeRequest
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.meldinger.FinnMeldekortUtbetalingsgrunnlagListeResponse
import org.slf4j.LoggerFactory
import java.time.LocalDate

class MeldekortClient(val port: MeldekortUtbetalingsgrunnlagV1) {

    private val log = LoggerFactory.getLogger("MeldekortClient")

    fun hentMeldekortgrunnlag(aktørId: String, fom: LocalDate, tom: LocalDate): Either<Exception, List<MeldekortUtbetalingsgrunnlagSak>> {
        return try {
            val request = FinnMeldekortUtbetalingsgrunnlagListeRequest()
                    .apply {
                        this.ident = AktoerId().apply {
                            this.aktoerId = aktørId
                        }
                        this.periode = Periode().apply {
                            this.fom = fom.toXmlGregorianCalendar()
                            this.tom = tom.toXmlGregorianCalendar()
                        }
                        this.temaListe.add(ObjectFactory().createTema().apply {
                            this.kodeverksRef = "DAG"
                            this.value = "DAG"
                        })
                        this.temaListe.add(ObjectFactory().createTema().apply {
                            this.kodeverksRef = "AAP"
                            this.value = "AAP"
                        })
                    }
            val response: FinnMeldekortUtbetalingsgrunnlagListeResponse = port.finnMeldekortUtbetalingsgrunnlagListe(request)
            Either.Right(response.meldekortUtbetalingsgrunnlagListe.flatMap(this::toSak))
        } catch (ex: Exception) {
            log.error("Error while doing meldekort lookup", ex)
            Either.Left(ex)
        }
    }

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
                type = sak.tema.kodeverksRef,
                kilde = "ARENA",
                saksnummer = sak.fagsystemSakId,
                saksstatus = sak.saksstatus.kodeverksRef,
                kravMottattDato = vedtak.datoKravMottatt.toLocalDate(),
                vedtaksstatus = vedtak.vedtaksstatus.kodeverksRef,
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
                type = sak.tema.kodeverksRef,
                kilde = "ARENA",
                saksnummer = sak.fagsystemSakId,
                saksstatus = sak.saksstatus.kodeverksRef
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

