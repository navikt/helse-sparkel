package no.nav.helse.ws.arbeidsforhold

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.MatchesXPathPattern
import no.nav.helse.ws.withSoapAction

fun finnArbeidsforholdPrArbeidstakerStub(ident: String): MappingBuilder {
    return WireMock.post(WireMock.urlPathEqualTo("/aareg"))
            .withSoapAction("http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/Arbeidsforhold_v3/finnArbeidsforholdPrArbeidstakerRequest")
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/ns2:finnArbeidsforholdPrArbeidstaker/parameters/ident/ident/text()",
                    aaregNamespace, WireMock.equalTo(ident)))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/ns2:finnArbeidsforholdPrArbeidstaker/parameters/rapportertSomRegelverk/text()",
                    aaregNamespace, WireMock.equalTo("ALLE")))
}

private val aaregNamespace = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "ns2" to "http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3"
)
