package no.nav.helse.oppslag.arbeidsforhold

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.MatchesXPathPattern
import no.nav.helse.oppslag.withSoapAction

fun finnArbeidsforholdPrArbeidstakerStub(ident: String, fom: String, tom: String): MappingBuilder {
    return WireMock.post(WireMock.urlPathEqualTo("/aareg"))
            .withSoapAction("http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/Arbeidsforhold_v3/finnArbeidsforholdPrArbeidstakerRequest")
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/ns2:finnArbeidsforholdPrArbeidstaker/parameters/ident/ident/text()",
                    aaregNamespace, WireMock.equalTo(ident)))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/ns2:finnArbeidsforholdPrArbeidstaker/parameters/arbeidsforholdIPeriode/fom/text()",
                    aaregNamespace, WireMock.equalTo(fom)))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/ns2:finnArbeidsforholdPrArbeidstaker/parameters/arbeidsforholdIPeriode/tom/text()",
                    aaregNamespace, WireMock.equalTo(tom)))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/ns2:finnArbeidsforholdPrArbeidstaker/parameters/rapportertSomRegelverk/text()",
                    aaregNamespace, WireMock.equalTo("A_ORDNINGEN")))
}

fun hentArbeidsforholdHistorikkStub(arbeidsforholdIDnav: String): MappingBuilder {
    return WireMock.post(WireMock.urlPathEqualTo("/aareg"))
            .withSoapAction("http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/Arbeidsforhold_v3/hentArbeidsforholdHistorikkRequest")
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/ns2:hentArbeidsforholdHistorikk/parameters/arbeidsforholdId/text()",
                    aaregNamespace, WireMock.equalTo(arbeidsforholdIDnav)))
}

private val aaregNamespace = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "ns2" to "http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3"
)
