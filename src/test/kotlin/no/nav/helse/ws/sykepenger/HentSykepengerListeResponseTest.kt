package no.nav.helse.ws.sykepenger

import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.KommendeVedtak
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Periode
import no.nav.tjeneste.virksomhet.sykepenger.v2.informasjon.Sykmeldingsperiode
import no.nav.tjeneste.virksomhet.sykepenger.v2.meldinger.HentSykepengerListeResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.*
import javax.xml.datatype.DatatypeFactory

class HentSykepengerListeResponseTest {

    @Test
    fun `should map a simple sykepengeliste`() {
        val xml = HentSykepengerListeResponse()
        val sykmeldingsperiode = Sykmeldingsperiode()
        val vedtak = KommendeVedtak().apply {
            this.vedtak = Periode().apply {
                this.fom = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2001, Month.JANUARY.value, 1, TimeZone.SHORT)
                this.tom = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2001, Month.JANUARY.value, 11, TimeZone.SHORT)
            }
            this.utbetalingsgrad = BigDecimal.TEN
        }
        sykmeldingsperiode.vedtakListe.add(vedtak)
        xml.sykmeldingsperiodeListe.add(sykmeldingsperiode)

        val transform: Collection<SykepengerVedtak> = xml.toSykepengerVedtak("en slags aktør")
        val transformertVedtak = transform.first()

        assertThat(transform).hasSize(1)
        assertThat(transformertVedtak.fom).isEqualTo(LocalDate.of(2001, Month.JANUARY, 1))
        assertThat(transformertVedtak.tom).isEqualTo(LocalDate.of(2001, Month.JANUARY, 11))
        assertThat(transformertVedtak.grad).isEqualTo(10.0f)
    }
}