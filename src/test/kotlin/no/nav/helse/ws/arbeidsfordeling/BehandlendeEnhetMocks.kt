package no.nav.helse.ws.arbeidsfordeling

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import no.nav.helse.ws.Responses
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import no.nav.helse.ws.withSoapAction

object BehandlendeEnhetMocks {
    fun mockMedGeografiskTilknytning(
            geografiskTilknytning : String,
            tema : String,
            xml: String
    ) {
        WireMock.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/arbeidsfordeling"))
                        .withSoapAction("http://nav.no/tjeneste/virksomhet/arbeidsfordeling/v1/Arbeidsfordeling_v1/finnBehandlendeEnhetListeRequest")
                        .withRequestBody(ContainsPattern("<geografiskTilknytning>$geografiskTilknytning</geografiskTilknytning>"))
                        .withRequestBody(ContainsPattern("<tema>$tema</tema>"))
                        .withSamlAssertion()
                        .withCallId()
                        .willReturn(WireMock.okXml(xml))
        )
    }

    fun mockKode6(
            tema : String,
            xml: String
    ) {
        WireMock.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/arbeidsfordeling"))
                        .withSoapAction("http://nav.no/tjeneste/virksomhet/arbeidsfordeling/v1/Arbeidsfordeling_v1/finnBehandlendeEnhetListeRequest")
                        .withRequestBody(ContainsPattern("<diskresjonskode>SPSF</diskresjonskode>"))
                        .withRequestBody(ContainsPattern("<tema>$tema</tema>"))
                        .withSamlAssertion()
                        .withCallId()
                        .willReturn(WireMock.okXml(xml))
        )
    }

    fun mockKode7(
            geografiskTilknytning : String,
            tema : String,
            xml: String
    ) {
        WireMock.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/arbeidsfordeling"))
                        .withSoapAction("http://nav.no/tjeneste/virksomhet/arbeidsfordeling/v1/Arbeidsfordeling_v1/finnBehandlendeEnhetListeRequest")
                        .withRequestBody(ContainsPattern("<diskresjonskode>SPFO</diskresjonskode>"))
                        .withRequestBody(ContainsPattern("<geografiskTilknytning>$geografiskTilknytning</geografiskTilknytning>"))
                        .withRequestBody(ContainsPattern("<tema>$tema</tema>"))
                        .withSamlAssertion()
                        .withCallId()
                        .willReturn(WireMock.okXml(xml))
        )
    }

    fun mockBareTema(
            tema : String,
            xml: String
    ) {
        WireMock.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/arbeidsfordeling"))
                        .withSoapAction("http://nav.no/tjeneste/virksomhet/arbeidsfordeling/v1/Arbeidsfordeling_v1/finnBehandlendeEnhetListeRequest")
                        .withRequestBody(ContainsPattern("<arbeidsfordelingKriterier><tema>$tema</tema></arbeidsfordelingKriterier>"))
                        .withSamlAssertion()
                        .withCallId()
                        .willReturn(WireMock.okXml(xml))
        )
    }

    fun enAktivEnhetResponses(
            enhetId : String,
            enhetNavn: String
    ) : Responses {
        val xml = """
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
            <SOAP-ENV:Header xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"/>
            <soap:Body>
                <ns2:finnBehandlendeEnhetListeResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/arbeidsfordeling/v1/">
                    <response>
                        <behandlendeEnhetListe>
                            <enhetId>$enhetId</enhetId>
                            <enhetNavn>$enhetNavn</enhetNavn>
                            <status>AKTIV</status>
                            <type>YTA</type>
                        </behandlendeEnhetListe>
                    </response>
                </ns2:finnBehandlendeEnhetListeResponse>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()


        val json = """
            {
                "id": "$enhetId",
                "navn": "$enhetNavn"
            }
        """.trimIndent()

        return Responses(
                registerXmlResponse = xml,
                sparkelJsonResponse = json
        )
    }
}