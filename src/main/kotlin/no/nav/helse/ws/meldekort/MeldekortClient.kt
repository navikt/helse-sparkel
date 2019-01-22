package no.nav.helse.ws.meldekort

import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.ws.sykepenger.toLocalDate
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.MeldekortUtbetalingsgrunnlagV1
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.informasjon.*
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.meldinger.FinnMeldekortUtbetalingsgrunnlagListeRequest
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.meldinger.FinnMeldekortUtbetalingsgrunnlagListeResponse
import java.lang.Exception
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

class MeldekortClient(val port: MeldekortUtbetalingsgrunnlagV1) {
    fun hentMeldekortgrunnlag(aktørId: String, fom: LocalDate, tom: LocalDate): OppslagResult {
        try {
            val response: FinnMeldekortUtbetalingsgrunnlagListeResponse = port.finnMeldekortUtbetalingsgrunnlagListe(FinnMeldekortUtbetalingsgrunnlagListeRequest()
                    .apply {
                        this.ident = AktoerId().apply {
                            this.aktoerId = aktørId
                        }
                        this.periode = Periode().apply {
                            this.fom = fom.toXMLGregorian()
                            this.tom = tom.toXMLGregorian()
                        }
                        this.temaListe.add(Tema().apply { this.kodeverksRef = "DAG" })
                        this.temaListe.add(Tema().apply { this.kodeverksRef = "AAP" })
                    })

            return Success(response.meldekortUtbetalingsgrunnlagListe.flatMap(this::toSak))
        } catch(e: Exception) {
            return Failure(listOf("${e.javaClass.simpleName} : ${e.message}"))
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

private fun LocalDate.toXMLGregorian(): XMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(this.atStartOfDay(ZoneId.systemDefault())))
