package no.nav.helse.ws

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.MatchesXPathPattern
import com.github.tomakehurst.wiremock.stubbing.Scenario
import no.nav.helse.Environment
import no.nav.helse.Failure
import no.nav.helse.Success
import no.nav.helse.ws.person.Kjønn
import no.nav.helse.ws.person.Person
import no.nav.helse.ws.sts.STS_SAML_POLICY_NO_TRANSPORT_BINDING
import org.junit.jupiter.api.*
import java.time.LocalDate

class SoapIntegrationTest {

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }

    @Test
    fun `sts should be called before making soap call`() {
        val clients = Clients(Environment(mapOf(
                "SECURITY_TOKEN_SERVICE_URL" to server.baseUrl().plus("/sts"),
                "SECURITY_TOKEN_SERVICE_USERNAME" to "stsUsername",
                "SECURITY_TOKEN_SERVICE_PASSWORD" to "stsPassword",
                "PERSON_ENDPOINTURL" to server.baseUrl().plus("/person")
        )), STS_SAML_POLICY_NO_TRANSPORT_BINDING)

        WireMock.stubFor(stsRequestMapping
                .willReturn(WireMock.ok(sts_response))
                .inScenario("default")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("token acquired"))

        WireMock.stubFor(personRequestMapping
                .willReturn(WireMock.ok(hentPerson_response))
                .inScenario("default")
                .whenScenarioStateIs("token acquired")
                .willSetStateTo("personInfo called"))

        val actual = clients.personClient.personInfo(Fødselsnummer("08078422069"))
        val expected = Person(
                id = Fødselsnummer("08078422069"),
                fornavn = "JENNY",
                mellomnavn = "PIKENES",
                etternavn = "LOLNES",
                fdato = LocalDate.of(1984, 7, 8),
                kjønn = Kjønn.KVINNE
        )
        when (actual) {
            is Success<*> -> {
                Assertions.assertEquals(expected, actual.data)
            }
            is Failure -> fail { "This lookup was expected to succeed, but it didn't" }
        }
    }
}

val ns1 = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "wsse" to "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
        "saml2" to "urn:oasis:names:tc:SAML:2.0:assertion"
)

val ns2 = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "ns2" to "http://nav.no/tjeneste/virksomhet/person/v3",
        "ns3" to "http://nav.no/tjeneste/virksomhet/person/v3/informasjon"
)

private val personRequestMapping: MappingBuilder = WireMock.post(WireMock.urlPathEqualTo("/person"))
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
            ns2, WireMock.equalTo("08078422069")))

val ns3 = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "wsse" to "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
)

val ns4 = mapOf(
        "soap" to "http://schemas.xmlsoap.org/soap/envelope/",
        "wst" to "http://docs.oasis-open.org/ws-sx/ws-trust/200512"
)

private val stsRequestMapping: MappingBuilder = WireMock.post(WireMock.urlPathEqualTo("/sts"))
        .withHeader("Content-Type", WireMock.containing("text/xml"))
        .withHeader("SOAPAction", WireMock.equalTo("\"http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue\""))
        .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Header/wsse:Security/wsse:UsernameToken/wsse:Username/text()",
                ns3, WireMock.equalTo("stsUsername")))
        .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Header/wsse:Security/wsse:UsernameToken/wsse:Password/text()",
                ns3, WireMock.equalTo("stsPassword")))
        .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/wst:RequestSecurityToken/wst:SecondaryParameters/wst:SecondaryParameters/wst:TokenType/text()",
            ns4, WireMock.equalTo("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0")))
        .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/wst:RequestSecurityToken/wst:RequestType/text()",
            ns4, WireMock.equalTo("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue")))
        .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/wst:RequestSecurityToken/wst:SecondaryParameters/wst:TokenType/text()",
            ns4, WireMock.containing("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0")))
        .withRequestBody(MatchesXPathPattern("//soap:Envelope/soap:Body/wst:RequestSecurityToken/wst:SecondaryParameters/wst:KeyType/text()",
            ns4, WireMock.equalTo("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer")))

private val sts_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:wsa="http://www.w3.org/2005/08/addressing" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
    <soapenv:Header>
        <wsa:MessageID>urn:uuid:5e22dd04-7a8e-494a-a05e-2fff16ecf883</wsa:MessageID>
        <wsa:Action>http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTRC/IssueFinal</wsa:Action>
        <wsa:To>http://www.w3.org/2005/08/addressing/anonymous</wsa:To>
    </soapenv:Header>
    <soapenv:Body>
        <wst:RequestSecurityTokenResponseCollection xmlns:wst="http://docs.oasis-open.org/ws-sx/ws-trust/200512"
                                                    xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
            <wst:RequestSecurityTokenResponse Context="supportLater">
                <wst:TokenType>http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0</wst:TokenType>
                <wst:RequestedSecurityToken>
                    <saml2:Assertion Version="2.0" ID="SAML-8d11de08-b17f-45ba-bd18-68098a4d28ce" IssueInstant="2018-09-06T10:28:45Z"
                                     xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">
                        <saml2:Issuer>theIssuer</saml2:Issuer>
                        <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
                            <SignedInfo>
                                <CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                                <SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
                                <Reference URI="#SAML-8d11de08-b17f-45ba-bd18-68098a4d28ce">
                                    <Transforms>
                                        <Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
                                        <Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                                    </Transforms>
                                    <DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
                                    <DigestValue>digestValue</DigestValue>
                                </Reference>
                            </SignedInfo>
                            <SignatureValue>signatureValue</SignatureValue>
                            <KeyInfo>
                                <X509Data>
                                    <X509Certificate>certificateValue</X509Certificate>
                                    <X509IssuerSerial>

                                        <X509IssuerName>CN=B27 Issuing CA Intern, DC=preprod, DC=local</X509IssuerName>
                                        <X509SerialNumber>2363879011200190627239759946745671848168525609</X509SerialNumber>
                                    </X509IssuerSerial>
                                </X509Data>
                            </KeyInfo>
                        </Signature>
                        <saml2:Subject>
                            <saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified">testusername</saml2:NameID>
                            <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
                                <saml2:SubjectConfirmationData NotBefore="2018-09-06T10:28:42Z" NotOnOrAfter="2018-09-06T11:28:48Z"/>
                            </saml2:SubjectConfirmation>
                        </saml2:Subject>
                        <saml2:Conditions NotBefore="2018-09-06T10:28:42Z" NotOnOrAfter="2018-09-06T11:28:48Z"/>
                        <saml2:AttributeStatement>
                            <saml2:Attribute Name="identType" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri">
                                <saml2:AttributeValue>Systemressurs</saml2:AttributeValue>
                            </saml2:Attribute>
                            <saml2:Attribute Name="authenticationLevel" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri">
                                <saml2:AttributeValue>0</saml2:AttributeValue>
                            </saml2:Attribute>
                            <saml2:Attribute Name="consumerId" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri">
                                <saml2:AttributeValue>testusername</saml2:AttributeValue>
                            </saml2:Attribute>
                        </saml2:AttributeStatement>
                    </saml2:Assertion>
                </wst:RequestedSecurityToken>
                <wst:Lifetime>
                    <wsu:Created>2018-09-06T10:28:42Z</wsu:Created>
                    <wsu:Expires>2018-09-06T11:28:48Z</wsu:Expires>
                </wst:Lifetime>
            </wst:RequestSecurityTokenResponse>
        </wst:RequestSecurityTokenResponseCollection>
    </soapenv:Body>
</soapenv:Envelope>
""".trimIndent()

private val hentPerson_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Header xmlns:wsa="http://www.w3.org/2005/08/addressing">
      <wsa:Action>http://nav.no/tjeneste/virksomhet/person/v3/Person_v3/hentPersonResponse</wsa:Action>
      <wsa:RelatesTo>uuid:242e017b-58e8-4f17-a7d7-7bd4978ca1cc</wsa:RelatesTo>
   </soapenv:Header>
   <soapenv:Body>
      <ns2:hentPersonResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
         <response>
            <person xsi:type="ns3:Bruker" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
               <bostedsadresse endringstidspunkt="2018-11-07T00:00:00.000+01:00" endretAv="AJOURHD, SKD" endringstype="endret">
                  <strukturertAdresse xsi:type="ns3:Gateadresse">
                     <landkode>NOR</landkode>
                     <tilleggsadresseType>Offisiell adresse</tilleggsadresseType>
                     <poststed>0557</poststed>
                     <kommunenummer>0301</kommunenummer>
                     <gatenavn>SANNERGATA</gatenavn>
                     <husnummer>2</husnummer>
                  </strukturertAdresse>
               </bostedsadresse>
               <sivilstand endringstidspunkt="2018-11-07T00:00:00.000+01:00" endretAv="AJOURHD, SKD" endringstype="endret" fomGyldighetsperiode="2018-03-01T00:00:00.000+01:00">
                  <sivilstand>NULL</sivilstand>
               </sivilstand>
               <statsborgerskap endringstidspunkt="2018-11-07T00:00:00.000+01:00" endretAv="AJOURHD, SKD" endringstype="endret">
                  <land>NOR</land>
               </statsborgerskap>
               <aktoer xsi:type="ns3:PersonIdent">
                  <ident>
                     <ident>08078422069</ident>
                     <type>FNR</type>
                  </ident>
               </aktoer>
               <kjoenn>
                  <kjoenn>K</kjoenn>
               </kjoenn>
               <personnavn endringstidspunkt="2018-11-07T00:00:00.000+01:00" endretAv="AJOURHD, SKD" endringstype="endret">
                  <etternavn>LOLNES</etternavn>
                  <fornavn>JENNY</fornavn>
                  <mellomnavn>PIKENES</mellomnavn>
                  <sammensattNavn>LOLNES JENNY PIKENES</sammensattNavn>
               </personnavn>
               <personstatus endringstidspunkt="2018-11-07T00:00:00.000+01:00" endretAv="SKD" endringstype="endret">
                  <personstatus>BOSA</personstatus>
               </personstatus>
               <foedselsdato>
                  <foedselsdato>1984-07-08+02:00</foedselsdato>
               </foedselsdato>
               <gjeldendePostadressetype>BOSTEDSADRESSE</gjeldendePostadressetype>
               <geografiskTilknytning xsi:type="ns3:Bydel">
                  <geografiskTilknytning>030103</geografiskTilknytning>
               </geografiskTilknytning>
            </person>
         </response>
      </ns2:hentPersonResponse>
   </soapenv:Body>
</soapenv:Envelope>
""".trimIndent()
