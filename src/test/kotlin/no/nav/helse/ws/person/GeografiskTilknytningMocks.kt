package no.nav.helse.ws.person

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import no.nav.helse.ws.Responses
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import no.nav.helse.ws.withSoapAction

object GeografiskTilknytningMocks {
    fun mock(
            aktoerId: String,
            xml: String
    ) {
        WireMock.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/person"))
                .withSoapAction("http://nav.no/tjeneste/virksomhet/person/v3/Person_v3/hentGeografiskTilknytningRequest")
                .withRequestBody(ContainsPattern("<aktoerId>$aktoerId</aktoerId>"))
                .withSamlAssertion()
                .withCallId()
                .willReturn(WireMock.okXml(xml))
        )
    }

    fun medGeografiskTilknytningResponses(
            aktoerId: String,
            geografiskOmraadeType : String,
            geografiskOmraadeKode : String
    ) : Responses {
        val xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
            <soapenv:Body>
                <ns2:hentGeografiskTilknytningResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
                    <response>
                        <aktoer xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns3:AktoerId">
                            <aktoerId>$aktoerId</aktoerId>
                        </aktoer>
                        <navn>
                            <etternavn>LOLNES</etternavn>
                            <fornavn>JENNY</fornavn>
                            <mellomnavn>PIKENES</mellomnavn>
                            <sammensattNavn>LOLNES JENNY PIKENES</sammensattNavn>
                        </navn>
                        <geografiskTilknytning xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns3:$geografiskOmraadeType">
                            <geografiskTilknytning>$geografiskOmraadeKode</geografiskTilknytning>
                        </geografiskTilknytning>
                    </response>
                </ns2:hentGeografiskTilknytningResponse>
            </soapenv:Body>
        </soapenv:Envelope>
        """.trimIndent()

        val json = """
        {
            "type": "${geografiskOmraadeType.toUpperCase()}",
            "kode": "$geografiskOmraadeKode"
        }
        """.trimIndent()

        return Responses(
                registerXmlResponse = xml,
                sparkelJsonResponse = json
        )
    }

    fun medDiskresjonsKode6Responses(
            aktoerId: String
    ) : Responses {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Body>
                    <ns2:hentGeografiskTilknytningResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
                        <response>
                            <aktoer xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns3:AktoerId">
                                <aktoerId>$aktoerId</aktoerId>
                            </aktoer>
                            <navn>
                                <etternavn>DAME</etternavn>
                                <fornavn>SKJERMET</fornavn>
                                <sammensattNavn>DAME SKJERMET</sammensattNavn>
                            </navn>
                            <diskresjonskode>SPSF</diskresjonskode>
                        </response>
                    </ns2:hentGeografiskTilknytningResponse>
                </soapenv:Body>
            </soapenv:Envelope>
        """.trimIndent()

        val json = """
        {
            "error": "Ikke tilgang til å se geografisk tilknytning til denne aktøren."
        }
        """.trimIndent()

        return Responses(
                registerXmlResponse = xml,
                sparkelJsonResponse = json
        )
    }

    fun medDiskresjonsKode7Responses(
            aktoerId: String,
            geografiskOmraadeType : String,
            geografiskOmraadeKode : String
    ) : Responses {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Body>
                    <ns2:hentGeografiskTilknytningResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
                        <response>
                            <aktoer xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns3:AktoerId">
                                <aktoerId>$aktoerId</aktoerId>
                            </aktoer>
                            <navn>
                                <etternavn>DORULL</etternavn>
                                <fornavn>STOR</fornavn>
                                <sammensattNavn>DORULL STOR</sammensattNavn>
                            </navn>
                            <diskresjonskode>SPFO</diskresjonskode>
                            <geografiskTilknytning xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns3:$geografiskOmraadeType">
                                <geografiskTilknytning>$geografiskOmraadeKode</geografiskTilknytning>
                            </geografiskTilknytning>
                        </response>
                    </ns2:hentGeografiskTilknytningResponse>
                </soapenv:Body>
            </soapenv:Envelope>
        """.trimIndent()

        val json = """
        {
            "type": "${geografiskOmraadeType.toUpperCase()}",
            "kode": "$geografiskOmraadeKode"
        }
        """.trimIndent()

        return Responses(
                registerXmlResponse = xml,
                sparkelJsonResponse = json
        )
    }

    fun utenGeografiskTilknytningEllerDiskresjonskode(
            aktoerId: String
    ) : Responses {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                <soapenv:Body>
                    <ns2:hentGeografiskTilknytningResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
                        <response>
                            <aktoer xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns3:AktoerId">
                                <aktoerId>$aktoerId</aktoerId>
                            </aktoer>
                            <navn>
                                <etternavn>DAME</etternavn>
                                <fornavn>SKJERMET</fornavn>
                                <sammensattNavn>DAME SKJERMET</sammensattNavn>
                            </navn>
                        </response>
                    </ns2:hentGeografiskTilknytningResponse>
                </soapenv:Body>
            </soapenv:Envelope>
        """.trimIndent()

        val json = """
        {
            "error": "Aktøren har ingen geografisk tilknytning."
        }
        """.trimIndent()

        return Responses(
                registerXmlResponse = xml,
                sparkelJsonResponse = json
        )
    }
}