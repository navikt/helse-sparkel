package no.nav.helse.ws.sakogbehandling

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.MatchesXPathPattern
import no.nav.helse.ws.withSoapAction

fun finnSakOgBehandlingskjedeListeStub(ident: String): MappingBuilder {
    return WireMock.post(WireMock.urlPathEqualTo("/sakogbehandling"))
            .withSoapAction("http://nav.no/tjeneste/virksomhet/sakOgBehandling/v1/SakOgBehandling_v1/finnSakOgBehandlingskjedeListeRequest")
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/v1:finnSakOgBehandlingskjedeListe/request/aktoerREF/text()",
                    sakOgBehandlingNamespace, WireMock.equalTo(ident)))
}

private val sakOgBehandlingNamespace = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "v1" to "http://nav.no/tjeneste/virksomhet/sakOgBehandling/v1"
)
