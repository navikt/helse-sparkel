package no.nav.helse.domene.arbeid

import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.arbeid.domain.Arbeidsforhold
import no.nav.helse.domene.inntekt.domain.Virksomhet
import no.nav.helse.domene.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.AapenPeriode
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsforholdFrilanser
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Yrker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArbeidDomainMapperTest {

    @Test
    fun `yrke for frilansforhold kan være null`() {
        val fom = LocalDate.parse("2019-01-01")
        val tom = fom.plusMonths(3)
        val orgnr = "889640782"

        val arbeidsforhold = ArbeidsforholdFrilanser().apply {
            arbeidsgiver = Organisasjon().apply {
                orgnummer = orgnr
            }
            frilansPeriode = AapenPeriode().apply {
                this.fom = fom.toXmlGregorianCalendar()
                this.tom = tom.toXmlGregorianCalendar()
            }
        }

        val expected = Arbeidsforhold.Frilans(Virksomhet.Organisasjon(Organisasjonsnummer(orgnr)), fom, tom, "UKJENT")

        assertEquals(expected, ArbeidDomainMapper.toArbeidsforhold(arbeidsforhold))
    }

    @Test
    fun `yrke for frilansforhold kan være en tom string`() {
        val fom = LocalDate.parse("2019-01-01")
        val tom = fom.plusMonths(3)
        val orgnr = "889640782"

        val arbeidsforhold = ArbeidsforholdFrilanser().apply {
            arbeidsgiver = Organisasjon().apply {
                orgnummer = orgnr
            }
            frilansPeriode = AapenPeriode().apply {
                this.fom = fom.toXmlGregorianCalendar()
                this.tom = tom.toXmlGregorianCalendar()
            }
            yrke = Yrker().apply {
                value = ""
            }
        }

        val expected = Arbeidsforhold.Frilans(Virksomhet.Organisasjon(Organisasjonsnummer(orgnr)), fom, tom, "UKJENT")

        assertEquals(expected, ArbeidDomainMapper.toArbeidsforhold(arbeidsforhold))
    }

    @Test
    fun `tom for frilansforhold kan være null`() {
        val fom = LocalDate.parse("2019-01-01")
        val yrke = "Butikkmedarbeider"
        val orgnr = "889640782"

        val arbeidsforhold = ArbeidsforholdFrilanser().apply {
            arbeidsgiver = Organisasjon().apply {
                orgnummer = orgnr
            }
            frilansPeriode = AapenPeriode().apply {
                this.fom = fom.toXmlGregorianCalendar()
            }
            this.yrke = Yrker().apply {
                value = yrke
            }
        }

        val expected = Arbeidsforhold.Frilans(Virksomhet.Organisasjon(Organisasjonsnummer(orgnr)), fom, null, yrke)

        assertEquals(expected, ArbeidDomainMapper.toArbeidsforhold(arbeidsforhold))
    }
}
