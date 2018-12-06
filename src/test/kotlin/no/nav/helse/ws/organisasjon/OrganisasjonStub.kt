package no.nav.helse.ws.organisasjon

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.MatchesXPathPattern
import no.nav.helse.ws.withSoapAction

fun hentOrganisasjonStub(orgNr: String): MappingBuilder {
    return WireMock.post("/organisasjon")
            .withSoapAction("http://nav.no/tjeneste/virksomhet/organisasjon/v5/BindinghentOrganisasjon/")
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/*[local-name() = 'hentOrganisasjon']/request/orgnummer/text()",
                    organisasjonNamespace, WireMock.equalTo(orgNr)))
}

private val organisasjonNamespace = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "ns2" to "http://nav.no/tjeneste/virksomhet/organisasjon"
)
