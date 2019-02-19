package no.nav.helse.ws.organisasjon

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import no.nav.helse.ws.Responses
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import no.nav.helse.ws.withSoapAction

object OrganisasjonMocks {
    fun mock(
        orgNr: String,
        xml: String
    ) {
        WireMock.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/organisasjon"))
                        .withSoapAction("http://nav.no/tjeneste/virksomhet/organisasjon/v5/BindinghentNoekkelinfoOrganisasjon/")
                        .withRequestBody(ContainsPattern("<orgnummer>$orgNr</orgnummer>"))
                        .withSamlAssertion()
                        .withCallId()
                        .willReturn(WireMock.okXml(xml))
        )
    }

    fun okResponses(
        orgNr: String,
        navnLinje1: String = "",
        navnLinje2: String = "",
        navnLinje3: String = "",
        navnLinje4: String = "",
        navnLinje5: String = "",
        forventetReturNavn: String? = null
    ) : Responses {
        val xml =  """
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
            <soap:Body>
                <ns2:hentNoekkelinfoOrganisasjonResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/organisasjon/v5">
                    <response>
                        <orgnummer>$orgNr</orgnummer>
                        <navn xmlns:ns4="http://nav.no/tjeneste/virksomhet/organisasjon/v5/informasjon" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns4:UstrukturertNavn">
                            <navnelinje>$navnLinje1</navnelinje>
                            <navnelinje>$navnLinje2</navnelinje>
                            <navnelinje>$navnLinje3</navnelinje>
                            <navnelinje>$navnLinje4</navnelinje>
                            <navnelinje>$navnLinje5</navnelinje>
                        </navn>
                        <adresse xmlns:ns4="http://nav.no/tjeneste/virksomhet/organisasjon/v5/informasjon" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" fomBruksperiode="2015-02-23T10:38:34.403+01:00" fomGyldighetsperiode="2013-07-09T00:00:00.000+02:00" xsi:type="ns4:SemistrukturertAdresse">
                            <landkode kodeRef="NO"/>
                            <adresseledd>
                                <noekkel kodeRef="adresselinje1"/>
                                <verdi>Karl Johans gate 22</verdi>
                            </adresseledd>
                            <adresseledd>
                                <noekkel kodeRef="postnr"/>
                                <verdi>0026</verdi>
                            </adresseledd>
                            <adresseledd>
                                <noekkel kodeRef="kommunenr"/>
                                <verdi>0301</verdi>
                            </adresseledd>
                        </adresse>
                        <enhetstype kodeRef="STAT"/>
                    </response>
                </ns2:hentNoekkelinfoOrganisasjonResponse>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()

        return if (forventetReturNavn == null) {
            Responses(
                    registerXmlResponse = xml,
                    sparkelJsonResponse = "{}"
            )
        } else  {
            Responses(
                    registerXmlResponse = xml,
                    sparkelJsonResponse = """
                        {
                            "navn" : "$forventetReturNavn"
                        }
                    """.trimIndent()
            )
        }
    }


    fun sparkelNotImplementedResponse() : String {
        return """
            {
                "feilmelding": "Støtter ikke alle etterspurte attributter."
            }
        """.trimIndent()
    }
}
