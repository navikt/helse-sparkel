package no.nav.helse.ws.person

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.MatchesXPathPattern

fun personStub(ident: String): MappingBuilder {
    return WireMock.post(WireMock.urlPathEqualTo("/person"))
            .withHeader("Content-Type", WireMock.containing("text/xml"))
            .withHeader("SOAPAction", WireMock.equalTo("\"http://nav.no/tjeneste/virksomhet/person/v3/Person_v3/hentPersonRequest\""))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Header/wsse:Security/saml2:Assertion/saml2:Issuer/text()",
                    ns1, WireMock.equalTo("theIssuer")))
            .withRequestBody(MatchesXPathPattern("//*[local-name()=\"DigestValue\"]/text()",
                    ns1, WireMock.equalTo("digestValue")))
            .withRequestBody(MatchesXPathPattern("//*[local-name()=\"SignatureValue\"]/text()",
                    ns1, WireMock.equalTo("signatureValue")))
            .withRequestBody(MatchesXPathPattern("//*[local-name()=\"X509Certificate\"]/text()",
                    ns1, WireMock.equalTo("certificateValue")))
            .withRequestBody(MatchesXPathPattern("//*[local-name()=\"X509IssuerName\"]/text()",
                    ns1, WireMock.equalTo("CN=B27 Issuing CA Intern, DC=preprod, DC=local")))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Header/wsse:Security/saml2:Assertion/saml2:Subject/saml2:NameID/text()",
                    ns1, WireMock.equalTo("testusername")))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Header/wsse:Security/saml2:Assertion/saml2:AttributeStatement/saml2:Attribute/saml2:AttributeValue/text()",
                    ns1, WireMock.equalTo("testusername")))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/ns2:hentPerson/request/aktoer/ident/ident/text()",
                    ns2, WireMock.equalTo(ident)))
}

private val ns1 = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "wsse" to "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
        "saml2" to "urn:oasis:names:tc:SAML:2.0:assertion"
)

private val ns2 = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "ns2" to "http://nav.no/tjeneste/virksomhet/person/v3",
        "ns3" to "http://nav.no/tjeneste/virksomhet/person/v3/informasjon"
)
