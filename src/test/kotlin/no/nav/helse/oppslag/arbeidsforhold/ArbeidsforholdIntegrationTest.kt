package no.nav.helse.oppslag.arbeidsforhold

import arrow.core.Try
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.stubbing.Scenario
import no.nav.helse.common.toLocalDate
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.domene.AktørId
import no.nav.helse.oppslag.*
import no.nav.helse.oppslag.sts.STS_SAML_POLICY_NO_TRANSPORT_BINDING
import no.nav.helse.oppslag.sts.configureFor
import no.nav.helse.oppslag.sts.stsClient
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsavtale
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold
import org.junit.jupiter.api.*
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

class ArbeidsforholdIntegrationTest {

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
        val client = WireMock.create().port(server.port()).build()
        WireMock.configureFor(client)
        client.resetMappings()
    }

    @Test
    fun `skal svare med en liste over arbeidsgivere`() {

        val aktørId = AktørId("08088806280")
        val fom = LocalDate.parse("2017-01-01")
        val tom = LocalDate.parse("2019-01-01")

        val expected = listOf(
                Arbeidsforhold().apply {
                    arbeidsforholdID = "00000009692000000001"
                    arbeidsforholdIDnav = 38009429
                },
                Arbeidsforhold().apply {
                    arbeidsforholdID = "00000009692000000005"
                    arbeidsforholdIDnav = 44669403
                }
        )

        arbeidsforholdStub(
                server = server,
                scenario = "arbeidsforhold_hent_arbeidsforhold",
                request = finnArbeidsforholdPrArbeidstakerStub(aktørId.aktor, fom.toXmlGregorianCalendar().toXMLFormat(), tom.toXmlGregorianCalendar().toXMLFormat()),
                response = WireMock.okXml(finnArbeidsforholdPrArbeidstaker_response)
        ) { arbeidsforholdClient ->
            val actual = arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom)

            when (actual) {
                is Try.Success -> {
                    assertEquals(expected.size, actual.value.size)
                    expected.forEachIndexed { index, expectedArbeidsforhold ->
                        assertEquals(expectedArbeidsforhold.arbeidsforholdID, actual.value[index].arbeidsforholdID)
                        assertEquals(expectedArbeidsforhold.arbeidsforholdIDnav, actual.value[index].arbeidsforholdIDnav)
                    }
                }
                is Try.Failure -> fail { "Expected Try.Success to be returned" }
            }
        }
    }

    @Test
    fun `skal svare med feil når tjenesten svarer med feil`() {

        val aktørId = AktørId("08088806280")
        val fom = LocalDate.parse("2017-01-01")
        val tom = LocalDate.parse("2019-01-01")

        arbeidsforholdStub(
                server = server,
                scenario = "arbeidsforhold_ugyldig_aktør",
                request = finnArbeidsforholdPrArbeidstakerStub(aktørId.aktor, fom.toXmlGregorianCalendar().toXMLFormat(), tom.toXmlGregorianCalendar().toXMLFormat()),
                response = WireMock.serverError().withHeader("Content-Type", "text/xml;charset=UTF-8").withBody(ugyldigAktørResponse(aktørId.aktor))
        ) { arbeidsforholdClient ->
            val actual = arbeidsforholdClient.finnArbeidsforhold(aktørId, fom, tom)

            when (actual) {
                is Try.Failure -> {
                    when (actual.exception) {
                        is FinnArbeidsforholdPrArbeidstakerUgyldigInput -> assertEquals("Person med ident: 08088806280 ble ikke funnet i kall mot AktørId", actual.exception.message)
                        else -> fail { "Expected FinnArbeidsforholdPrArbeidstakerUgyldigInput to be returned" }
                    }
                }
                is Try.Success -> fail { "Expected Try.Failure to be returned" }
            }
        }
    }

    @Test
    fun `skal hente historiske arbeidsavtaler`() {
        val arbeidsforholdId = 123456789L

        val expected = listOf(
            Arbeidsavtale().apply {
                fomGyldighetsperiode = LocalDate.parse("2017-01-01").toXmlGregorianCalendar()
            },
            Arbeidsavtale().apply {
                fomGyldighetsperiode = LocalDate.parse("2016-01-01").toXmlGregorianCalendar()
                tomGyldighetsperiode = LocalDate.parse("2016-12-31").toXmlGregorianCalendar()
            }
        )

        arbeidsforholdHistorikkStub(
                server = server,
                scenario = "arbeidsforhold_hent_arbeidsavtaler",
                request = hentArbeidsforholdHistorikkStub(arbeidsforholdId.toString()),
                response = WireMock.ok(finnHistorikkForArbeidsforhold_response_1)
        ) { arbeidsforholdClient ->
            val actual = arbeidsforholdClient.finnHistoriskeArbeidsavtaler(arbeidsforholdId)

            when (actual) {
                is Try.Success -> {
                    assertEquals(expected.size, actual.value.size)
                    expected.forEachIndexed { index, expectedAvtale ->
                        assertEquals(expectedAvtale.fomGyldighetsperiode.toLocalDate(), actual.value[index].fomGyldighetsperiode.toLocalDate())
                        assertEquals(expectedAvtale.tomGyldighetsperiode?.toLocalDate(), actual.value[index].tomGyldighetsperiode?.toLocalDate())
                    }
                }
                is Try.Failure -> fail { "Expected Try.Success to be returned" }
            }
        }
    }
}

fun arbeidsforholdStub(server: WireMockServer, scenario: String, request: MappingBuilder, response: ResponseDefinitionBuilder, test: (ArbeidsforholdClient) -> Unit) {
    val stsUsername = "stsUsername"
    val stsPassword = "stsPassword"

    val tokenSubject = "srvtestapp"
    val tokenIssuer = "Certificate Authority Inc"
    val tokenIssuerName = "CN=Certificate Authority Inc, DC=example, DC=com"
    val tokenDigest = "a random string"
    val tokenSignature = "yet another random string"
    val tokenCertificate = "one last random string"

    WireMock.stubFor(stsStub(stsUsername, stsPassword)
            .willReturn(samlAssertionResponse(tokenSubject, tokenIssuer, tokenIssuerName,
                    tokenDigest, tokenSignature, tokenCertificate))
            .inScenario(scenario)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("security_token_service_called"))

    val stsClientWs = stsClient(server.baseUrl().plus("/sts"), stsUsername to stsPassword)
    val callId = UUID.randomUUID().toString()

    WireMock.stubFor(request
            .withSamlAssertion(tokenSubject, tokenIssuer, tokenIssuerName,
                    tokenDigest, tokenSignature, tokenCertificate)
            .withCallId(callId)
            .willReturn(response)
            .inScenario(scenario)
            .whenScenarioStateIs("security_token_service_called")
            .willSetStateTo("arbeidsforhold_stub_called"))

    test(ArbeidsforholdClient(ArbeidsforholdFactory.create(server.baseUrl().plus("/aareg"), WsClientFactory {
        callId
    }).apply {
        stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
    }))

    WireMock.listAllStubMappings().mappings.forEach {
        WireMock.verify(RequestPatternBuilder.like(it.request))
    }
}

fun arbeidsforholdHistorikkStub(server: WireMockServer, scenario: String, request: MappingBuilder, response: ResponseDefinitionBuilder, test: (ArbeidsforholdClient) -> Unit) {
    val stsUsername = "stsUsername"
    val stsPassword = "stsPassword"

    val tokenSubject = "srvtestapp"
    val tokenIssuer = "Certificate Authority Inc"
    val tokenIssuerName = "CN=Certificate Authority Inc, DC=example, DC=com"
    val tokenDigest = "a random string"
    val tokenSignature = "yet another random string"
    val tokenCertificate = "one last random string"

    WireMock.stubFor(stsStub(stsUsername, stsPassword)
            .willReturn(samlAssertionResponse(tokenSubject, tokenIssuer, tokenIssuerName,
                    tokenDigest, tokenSignature, tokenCertificate))
            .inScenario(scenario)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("security_token_service_called"))

    val stsClientWs = stsClient(server.baseUrl().plus("/sts"), stsUsername to stsPassword)
    val callId = UUID.randomUUID().toString()

    WireMock.stubFor(request
            .withSamlAssertion(tokenSubject, tokenIssuer, tokenIssuerName,
                    tokenDigest, tokenSignature, tokenCertificate)
            .withCallId(callId)
            .willReturn(response)
            .inScenario(scenario)
            .whenScenarioStateIs("security_token_service_called")
            .willSetStateTo("arbeidsforhold_historikk_stub_called"))

    test(ArbeidsforholdClient(ArbeidsforholdFactory.create(server.baseUrl().plus("/aareg"), WsClientFactory {
        callId
    }).apply {
        stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
    }))

    WireMock.listAllStubMappings().mappings.forEach {
        WireMock.verify(RequestPatternBuilder.like(it.request))
    }
}

private val finnArbeidsforholdPrArbeidstaker_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:finnArbeidsforholdPrArbeidstakerResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3">
         <parameters>
            <arbeidsforhold applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-01-05T13:31:11.214+01:00" opphavREF="eda00000-0000-0000-0000-000972417562" opprettelsestidspunkt="2015-10-05T14:00:16.809+02:00" opprettetAv="srvappserver" sistBekreftet="2018-01-05T13:29:41.000+01:00">
               <arbeidsforholdID>00000009692000000001</arbeidsforholdID>
               <arbeidsforholdIDnav>38009429</arbeidsforholdIDnav>
               <ansettelsesPeriode applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-01-05T13:31:11.177+01:00" fomBruksperiode="2018-01-05+01:00" opphavREF="eda00000-0000-0000-0000-000972417562">
                  <periode>
                     <fom>2015-09-19T00:00:00.000+02:00</fom>
                     <tom>2017-11-30T00:00:00.000+01:00</tom>
                  </periode>
               </ansettelsesPeriode>
               <arbeidsforholdstype kodeRef="ordinaertArbeidsforhold">Ordinært arbeidsforhold</arbeidsforholdstype>
               <antallTimerForTimeloennet applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2015-10-05T14:00:16.809+02:00" opphavREF="06c8a5b7-2d59-44fd-9f98-79cc4fc44a65">
                  <periode>
                     <fom>2015-09-01T00:00:00.000+02:00</fom>
                     <tom>2015-09-30T00:00:00.000+02:00</tom>
                  </periode>
                  <antallTimer>2.0</antallTimer>
                  <rapporteringsperiode>2015-09+02:00</rapporteringsperiode>
               </antallTimerForTimeloennet>
               <antallTimerForTimeloennet applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2015-11-05T12:48:16.406+01:00" opphavREF="35e5241a-425e-44b2-b32e-50c17bac570e">
                  <periode>
                     <fom>2015-10-01T00:00:00.000+02:00</fom>
                     <tom>2015-10-31T00:00:00.000+01:00</tom>
                  </periode>
                  <antallTimer>15.8</antallTimer>
                  <rapporteringsperiode>2015-10+02:00</rapporteringsperiode>
               </antallTimerForTimeloennet>
               <antallTimerForTimeloennet applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2015-12-04T15:23:27.334+01:00" opphavREF="092da794-e540-48af-85de-05b66c330138">
                  <periode>
                     <fom>2015-11-01T00:00:00.000+01:00</fom>
                     <tom>2015-11-30T00:00:00.000+01:00</tom>
                  </periode>
                  <antallTimer>8.0</antallTimer>
                  <rapporteringsperiode>2015-11+01:00</rapporteringsperiode>
               </antallTimerForTimeloennet>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-01-05T13:31:11.177+01:00" fomBruksperiode="2018-01-05+01:00" fomGyldighetsperiode="2017-12-01T00:00:00.000+01:00" opphavREF="eda00000-0000-0000-0000-000972417562">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>100.0</stillingsprosent>
                  <sisteLoennsendringsdato>2017-12-01+01:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>37.5</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsgiver xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>913548221</orgnummer>
               </arbeidsgiver>
               <arbeidstaker>
                  <ident>
                     <ident>07067625948</ident>
                  </ident>
               </arbeidstaker>
               <opplysningspliktig xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>913548221</orgnummer>
               </opplysningspliktig>
               <arbeidsforholdInnrapportertEtterAOrdningen>true</arbeidsforholdInnrapportertEtterAOrdningen>
            </arbeidsforhold>
            <arbeidsforhold applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-05-08T11:24:25.569+02:00" opphavREF="eda00000-0000-0000-0000-001190431060" opprettelsestidspunkt="2018-02-07T11:44:40.548+01:00" opprettetAv="srvappserver" sistBekreftet="2018-05-08T11:13:39.000+02:00">
               <arbeidsforholdID>00000009692000000005</arbeidsforholdID>
               <arbeidsforholdIDnav>44669403</arbeidsforholdIDnav>
               <ansettelsesPeriode applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-02-07T11:44:40.548+01:00" fomBruksperiode="2018-02-07+01:00" opphavREF="eda00000-0000-0000-0000-001041599767">
                  <periode>
                     <fom>2015-09-19T00:00:00.000+02:00</fom>
                     <tom>2017-10-31T00:00:00.000+01:00</tom>
                  </periode>
               </ansettelsesPeriode>
               <arbeidsforholdstype kodeRef="ordinaertArbeidsforhold">Ordinært arbeidsforhold</arbeidsforholdstype>
               <arbeidsavtale applikasjonsID="EDAG" endretAv="srvappserver" endringstidspunkt="2018-02-07T11:44:40.548+01:00" fomBruksperiode="2018-02-07+01:00" fomGyldighetsperiode="2018-01-01T00:00:00.000+01:00" opphavREF="eda00000-0000-0000-0000-001041599767">
                  <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                  <avloenningstype kodeRef="time">&lt;Fant ingen gyldig term for kode: time></avloenningstype>
                  <yrke kodeRef="8323102">SJÅFØR (LASTEBIL)</yrke>
                  <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>
                  <stillingsprosent>0.0</stillingsprosent>
                  <sisteLoennsendringsdato>2015-09-19+02:00</sisteLoennsendringsdato>
                  <beregnetAntallTimerPrUke>0.0</beregnetAntallTimerPrUke>
               </arbeidsavtale>
               <arbeidsgiver xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>984054564</orgnummer>
               </arbeidsgiver>
               <arbeidstaker>
                  <ident>
                     <ident>07067625948</ident>
                  </ident>
               </arbeidstaker>
               <opplysningspliktig xsi:type="ns4:Organisasjon" xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <orgnummer>984054564</orgnummer>
               </opplysningspliktig>
               <arbeidsforholdInnrapportertEtterAOrdningen>true</arbeidsforholdInnrapportertEtterAOrdningen>
            </arbeidsforhold>
         </parameters>
      </ns2:finnArbeidsforholdPrArbeidstakerResponse>
   </soap:Body>
</soap:Envelope>
""".trimIndent()

private val finnHistorikkForArbeidsforhold_response_1 = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body>
        <ns2:hentArbeidsforholdHistorikkResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3">
            <parameters>
                <arbeidsforhold applikasjonsID="AAREG" endretAv="Z990603" endringstidspunkt="2019-01-21T10:36:17.129+01:00" opphavREF="0475917" opprettelsestidspunkt="2019-01-21T10:36:17.129+01:00" opprettetAv="Z990603" sistBekreftet="2019-01-21T10:36:17.000+01:00">
                    <arbeidsforholdID>7132068136</arbeidsforholdID>
                    <arbeidsforholdIDnav>46590219</arbeidsforholdIDnav>
                    <ansettelsesPeriode applikasjonsID="AAREG" endretAv="Z990603" endringstidspunkt="2019-01-21T10:36:17.129+01:00" fomBruksperiode="2019-01-21+01:00" opphavREF="0475917">
                        <periode>
                            <fom>2016-01-01T00:00:00.000+01:00</fom>
                        </periode>
                    </ansettelsesPeriode>
                    <arbeidsforholdstype kodeRef="ordinaertArbeidsforhold">Ordinært arbeidsforhold</arbeidsforholdstype>
                    <arbeidsavtale applikasjonsID="AAREG" endretAv="Z990603" endringstidspunkt="2019-01-21T10:36:17.129+01:00" fomBruksperiode="2019-01-21+01:00" fomGyldighetsperiode="2017-01-01T00:00:00.000+01:00" opphavREF="0475917">
                        <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                        <avloenningstype kodeRef="fast">Fastlønn</avloenningstype>
                        <yrke kodeRef="0017561">KINOMASKINIST</yrke>
                        <avtaltArbeidstimerPerUke>40.0</avtaltArbeidstimerPerUke>
                        <stillingsprosent>100.0</stillingsprosent>
                        <beregnetAntallTimerPrUke>40.0</beregnetAntallTimerPrUke>
                    </arbeidsavtale>
                    <arbeidsavtale applikasjonsID="AAREG" endretAv="Z990603" endringstidspunkt="2019-01-21T10:36:17.129+01:00" fomBruksperiode="2019-01-21+01:00" fomGyldighetsperiode="2016-01-01T00:00:00.000+01:00" tomGyldighetsperiode="2016-12-31T00:00:00.000+01:00" opphavREF="0475917">
                        <arbeidstidsordning kodeRef="ikkeSkift">Ikke skift</arbeidstidsordning>
                        <avloenningstype kodeRef="fast">Fastlønn</avloenningstype>
                        <yrke kodeRef="0017561">KINOMASKINIST</yrke>
                        <avtaltArbeidstimerPerUke>40.0</avtaltArbeidstimerPerUke>
                        <stillingsprosent>100.0</stillingsprosent>
                        <beregnetAntallTimerPrUke>40.0</beregnetAntallTimerPrUke>
                    </arbeidsavtale>
                    <arbeidsgiver xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns4:Organisasjon">
                        <orgnummer>995298775</orgnummer>
                    </arbeidsgiver>
                    <arbeidstaker>
                        <ident>
                            <ident>08088806280</ident>
                        </ident>
                    </arbeidstaker>
                    <opplysningspliktig xmlns:ns4="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns4:Organisasjon">
                        <orgnummer>889640782</orgnummer>
                    </opplysningspliktig>
                    <arbeidsforholdInnrapportertEtterAOrdningen>true</arbeidsforholdInnrapportertEtterAOrdningen>
                </arbeidsforhold>
            </parameters>
        </ns2:hentArbeidsforholdHistorikkResponse>
    </soap:Body>
</soap:Envelope>
""".trimIndent()

fun ugyldigAktørResponse(aktør: String) = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body>
        <soap:Fault>
            <faultcode>soap:Server</faultcode>
            <faultstring>Person med ident: $aktør ble ikke funnet i kall mot AktørId</faultstring>
            <detail>
                <ns2:finnArbeidsforholdPrArbeidstakerugyldigInput xmlns:ns2="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3">
                    <feilkilde>Aareg.core</feilkilde>
                    <feilaarsak>Person med ident: $aktør ble ikke funnet i kall mot AktørId</feilaarsak>
                    <feilmelding>Person med ident: $aktør ble ikke funnet i kall mot AktørId</feilmelding>
                    <tidspunkt>2019-02-21T20:30:40.483+01:00</tidspunkt>
                </ns2:finnArbeidsforholdPrArbeidstakerugyldigInput>
            </detail>
        </soap:Fault>
    </soap:Body>
</soap:Envelope>
""".trimIndent()
