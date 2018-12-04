package no.nav.helse.ws.arbeidsforhold

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.*
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.*
import javax.xml.datatype.DatatypeFactory

class ArbeidsforholdSOAPStub : ArbeidsforholdV3 {
    override fun hentArbeidsforholdHistorikk(parameters: HentArbeidsforholdHistorikkRequest?): HentArbeidsforholdHistorikkResponse {
        TODO("not implemented")
    }

    override fun finnArbeidsforholdPrArbeidsgiver(parameters: FinnArbeidsforholdPrArbeidsgiverRequest?): FinnArbeidsforholdPrArbeidsgiverResponse {
        TODO("not implemented")
    }

    override fun finnArbeidsforholdPrArbeidstaker(parameters: FinnArbeidsforholdPrArbeidstakerRequest?): FinnArbeidsforholdPrArbeidstakerResponse {
        return FinnArbeidsforholdPrArbeidstakerResponse().apply {
            this.arbeidsforhold.add(Arbeidsforhold().apply {
                this.ansettelsesPeriode = AnsettelsesPeriode().apply {
                    this.applikasjonsID = "some ansettelsesperiodeapplikasjonsid"
                    this.endretAv = "en saksbehandler, antagelig"
                    this.endringstidspunkt = DatatypeFactory.newInstance().newXMLGregorianCalendar()
                    this.fomBruksperiode = DatatypeFactory.newInstance().newXMLGregorianCalendar()
                    this.tomBruksperiode = DatatypeFactory.newInstance().newXMLGregorianCalendar()
                    this.opphavREF = "et opphav"
                }
                this.arbeidstaker = Person().apply { this.ident = NorskIdent().apply { this.ident = "12121212345" } }.apply { this.aktoerId = "abcd" }
                this.arbeidsgiver = Organisasjon().apply { this.aktoerId = "1234" }.apply { this.orgnummer = "et orgnummer" }
                this.arbeidsforholdID = "Én"
                this.arbeidsforholdstype = Arbeidsforholdstyper().apply { this.kodeverksRef = "en type arbeidsforhold" }
            })
        }
    }

    override fun finnArbeidstakerePrArbeidsgiver(parameters: FinnArbeidstakerePrArbeidsgiverRequest?): FinnArbeidstakerePrArbeidsgiverResponse {
        TODO("not implemented")
    }

    override fun ping() {
        TODO("not implemented")
    }

}
