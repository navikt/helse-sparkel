package no.nav.helse.ws.person

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.MatchesXPathPattern
import no.nav.helse.ws.withSoapAction

fun hentPersonStub(ident: String): MappingBuilder {
    return WireMock.post(WireMock.urlPathEqualTo("/person"))
            .withSoapAction("http://nav.no/tjeneste/virksomhet/person/v3/Person_v3/hentPersonRequest")
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/ns2:hentPerson/request/aktoer/ident/ident/text()",
                    personNamespace, WireMock.equalTo(ident)))
}

private val personNamespace = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "ns2" to "http://nav.no/tjeneste/virksomhet/person/v3",
        "ns3" to "http://nav.no/tjeneste/virksomhet/person/v3/informasjon"
)
