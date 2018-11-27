package no.nav.helse.ws.arbeidsforhold

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.*
import java.time.LocalDate
import javax.xml.datatype.DatatypeFactory

class ArbeidsforholdSOAPStub : ArbeidsforholdV3 {
    override fun finnArbeidsforholdPrArbeidsgiver(parameters: WSFinnArbeidsforholdPrArbeidsgiverRequest?): WSFinnArbeidsforholdPrArbeidsgiverResponse? {
        TODO("not implemented")
    }

    override fun finnArbeidsforholdPrArbeidstaker(parameters: WSFinnArbeidsforholdPrArbeidstakerRequest?): WSFinnArbeidsforholdPrArbeidstakerResponse {
        return WSFinnArbeidsforholdPrArbeidstakerResponse().apply {
            this.arbeidsforhold.add(WSArbeidsforhold().apply {
                this.ansettelsesPeriode = WSAnsettelsesPeriode().apply {
                    this.applikasjonsID = "some ansettelsesperiodeapplikasjonsid"
                    this.endretAv = "en saksbehandler, antagelig"
                    this.endringstidspunkt = LocalDate.now()
                    this.fomBruksperiode = DatatypeFactory.newInstance().newXMLGregorianCalendar()
                    this.tomBruksperiode = DatatypeFactory.newInstance().newXMLGregorianCalendar()
                    this.opphavREF = "et opphav"
                }
                this.arbeidstaker = WSPerson().apply { this.ident = WSNorskIdent().apply { this.ident = "12121212345" } }.apply { this.aktoerId = "abcd" }
                this.arbeidsgiver = WSOrganisasjon().apply { this.aktoerId = "1234" }.apply { this.orgnummer = "et orgnummer" }
                this.arbeidsforholdID = "Én"
                this.arbeidsforholdstype = WSArbeidsforholdstyper().apply { this.kodeverksRef = "en type arbeidsforhold" }
            })
        }
    }

    override fun hentArbeidsforholdHistorikk(parameters: WSHentArbeidsforholdHistorikkRequest?): WSHentArbeidsforholdHistorikkResponse {
        TODO("not implemented")
    }

    override fun ping() {
        TODO("not implemented")
    }

    override fun finnArbeidstakerePrArbeidsgiver(parameters: WSFinnArbeidstakerePrArbeidsgiverRequest?): WSFinnArbeidstakerePrArbeidsgiverResponse {
        TODO("not implemented")
    }

}
