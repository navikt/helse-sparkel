package no.nav.helse.oppslag

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.MatchesXPathPattern

fun MappingBuilder.withSoapAction(action: String): MappingBuilder {
    return withHeader("Content-Type", WireMock.containing("text/xml"))
            .withHeader("SOAPAction", WireMock.equalTo("\"$action\""))
}

fun MappingBuilder.withCallId(callId: String): MappingBuilder {
    val namespace = mapOf(
            "soap" to "http://schemas.xmlsoap.org/soap/envelope/"
    )
    return withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Header/*[local-name()='callId' and namespace-uri()='uri:no.nav.applikasjonsrammeverk']/text()", namespace, WireMock.equalTo(callId)))
}

fun MappingBuilder.withUsernamePasswordToken(username: String, password: String): MappingBuilder {
    return withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Header/wsse:Security/wsse:UsernameToken/wsse:Username/text()",
            wsseNamespace, WireMock.equalTo(username)))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Header/wsse:Security/wsse:UsernameToken/wsse:Password/text()",
                    wsseNamespace, WireMock.equalTo(password)))
}

fun MappingBuilder.withRequestSecurityTokenAssertion(): MappingBuilder {
    return withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/wst:RequestSecurityToken/wst:SecondaryParameters/wst:SecondaryParameters/wst:TokenType/text()",
            requestSecurityTokenNamespace, WireMock.equalTo("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0")))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/wst:RequestSecurityToken/wst:RequestType/text()",
                    requestSecurityTokenNamespace, WireMock.equalTo("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue")))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/wst:RequestSecurityToken/wst:SecondaryParameters/wst:TokenType/text()",
                    requestSecurityTokenNamespace, WireMock.containing("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0")))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/wst:RequestSecurityToken/wst:SecondaryParameters/wst:KeyType/text()",
                    requestSecurityTokenNamespace, WireMock.equalTo("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer")))
}

fun MappingBuilder.withSamlAssertion(
        username: String = "testusername",
        issuer: String = "theIssuer",
        issuerName: String = "CN=B27 Issuing CA Intern, DC=preprod, DC=local",
        digest: String = "digestValue",
        signature: String = "signatureValue",
        certificate: String = "certificateValue"
): MappingBuilder {
    return withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Header/wsse:Security/saml2:Assertion/saml2:Issuer/text()",
            saml2Namespace, WireMock.equalTo(issuer)))
            .withRequestBody(MatchesXPathPattern("//*[local-name()=\"DigestValue\"]/text()",
                    saml2Namespace, WireMock.equalTo(digest)))
            .withRequestBody(MatchesXPathPattern("//*[local-name()=\"SignatureValue\"]/text()",
                    saml2Namespace, WireMock.equalTo(signature)))
            .withRequestBody(MatchesXPathPattern("//*[local-name()=\"X509Certificate\"]/text()",
                    saml2Namespace, WireMock.equalTo(certificate)))
            .withRequestBody(MatchesXPathPattern("//*[local-name()=\"X509IssuerName\"]/text()",
                    saml2Namespace, WireMock.equalTo(issuerName)))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Header/wsse:Security/saml2:Assertion/saml2:Subject/saml2:NameID/text()",
                    saml2Namespace, WireMock.equalTo(username)))
            .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Header/wsse:Security/saml2:Assertion/saml2:AttributeStatement/saml2:Attribute/saml2:AttributeValue/text()",
                    saml2Namespace, WireMock.equalTo(username)))
}

private val wsseNamespace = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "wsse" to "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
)

private val requestSecurityTokenNamespace = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "wst" to "http://docs.oasis-open.org/ws-sx/ws-trust/200512"
)

private val saml2Namespace = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "wsse" to "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
        "saml2" to "urn:oasis:names:tc:SAML:2.0:assertion"
)
