package no.nav.helse.ws.sakogbehandling

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
import no.nav.helse.sts.StsRestClient
import no.nav.helse.ws.*
import no.nav.helse.ws.sts.stsClient
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.finnsakogbehandlingskjedeliste.Behandlingskjede
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.sakogbehandling.Behandlingsstatuser
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.sakogbehandling.Sakstemaer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SakOgBehandlingIntegrationTest {

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
    fun `skal gi liste over saker`() {
        val aktørId = "11987654321"

        sakOgBehandlingStub(
                server = server,
                scenario = "sak_og_behandling",
                request = finnSakOgBehandlingskjedeListeStub(aktørId),
                response = WireMock.okXml(finnSakOgBehandlingskjedeListe_response)
        ) { sakOgBehandlingClient ->
            val expected = listOf(
                    no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.finnsakogbehandlingskjedeliste.Sak().apply {
                        saksId = "010847146"
                        sakstema = Sakstemaer().apply {
                            value = "AAP"
                        }
                        opprettet = LocalDate.parse("2018-07-24").toXmlGregorianCalendar()
                        with(behandlingskjede) {
                            add (Behandlingskjede().apply {
                                slutt = LocalDate.parse("2018-09-19").toXmlGregorianCalendar()
                                sisteBehandlingsstatus = Behandlingsstatuser().apply {
                                    value = "avbrutt"
                                }
                            })
                        }
                    },
                    no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.finnsakogbehandlingskjedeliste.Sak().apply {
                        saksId = "010847171"
                        sakstema = Sakstemaer().apply {
                            value = "SYM"
                        }
                        opprettet = LocalDate.parse("2018-08-08").toXmlGregorianCalendar()
                        with(behandlingskjede) {
                            add (Behandlingskjede().apply {
                                slutt = LocalDate.parse("2018-11-19").toXmlGregorianCalendar()
                                sisteBehandlingsstatus = Behandlingsstatuser().apply {
                                    value = "avsluttet"
                                }
                            })
                            add (Behandlingskjede().apply {
                                slutt = LocalDate.parse("2018-11-19").toXmlGregorianCalendar()
                                sisteBehandlingsstatus = Behandlingsstatuser().apply {
                                    value = "avsluttet"
                                }
                            })
                        }
                    }
            )
            val actual = sakOgBehandlingClient.finnSakOgBehandling(aktørId)

            when (actual) {
                is Try.Success -> {
                    assertEquals(expected.size, actual.value.size)
                    expected.forEachIndexed { key, value ->
                        assertEquals(value.saksId, actual.value[key].saksId)
                        assertEquals(value.sakstema.value, actual.value[key].sakstema.value)
                        assertEquals(value.opprettet.toLocalDate(), actual.value[key].opprettet.toLocalDate())
                        assertEquals(value.behandlingskjede.size, actual.value[key].behandlingskjede.size)
                        value.behandlingskjede.forEachIndexed { index, behandlingskjede ->
                            assertEquals(behandlingskjede.slutt.toLocalDate(), actual.value[key].behandlingskjede[index].slutt.toLocalDate())
                            assertEquals(behandlingskjede.sisteBehandlingsstatus.value, actual.value[key].behandlingskjede[index].sisteBehandlingsstatus.value)
                        }
                    }
                }
                is Try.Failure -> fail { "Expected Try.Success to be returned" }
            }
        }
    }

    @Test
    fun `skal svare med feil når tjenesten feiler`() {
        val aktørId = "11987654321"

        sakOgBehandlingStub(
                server = server,
                scenario = "sak_og_behandling",
                request = finnSakOgBehandlingskjedeListeStub(aktørId),
                response = WireMock.serverError().withBody(faultXml("SOAP fault"))
        ) { sakOgBehandlingClient ->
            val actual = sakOgBehandlingClient.finnSakOgBehandling(aktørId)

            when (actual) {
                is Try.Failure -> assertEquals("SOAP fault", actual.exception.message)
                is Try.Success -> fail { "Expected Try.Failure to be returned" }
            }
        }
    }
}

fun sakOgBehandlingStub(server: WireMockServer, scenario: String, response: ResponseDefinitionBuilder, request: MappingBuilder, test: (SakOgBehandlingClient) -> Unit) {
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

    WireMock.stubFor(request
            .withSamlAssertion(tokenSubject, tokenIssuer, tokenIssuerName,
                    tokenDigest, tokenSignature, tokenCertificate)
            .withCallId("Sett inn call id her")
            .willReturn(response)
            .inScenario(scenario)
            .whenScenarioStateIs("security_token_service_called")
            .willSetStateTo("sak_og_behandling_stub_called"))

    val stsClientWs = stsClient(server.baseUrl().plus("/sts"), stsUsername to stsPassword)
    val stsClientRest = StsRestClient(server.baseUrl().plus("/sts"), stsUsername, stsPassword)

    val wsClients = WsClients(stsClientWs, stsClientRest, true)

    test(wsClients.sakOgBehandling(server.baseUrl().plus("/sakogbehandling")))

    WireMock.listAllStubMappings().mappings.forEach {
        WireMock.verify(RequestPatternBuilder.like(it.request))
    }
}

private val finnSakOgBehandlingskjedeListe_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:finnSakOgBehandlingskjedeListeResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/sakOgBehandling/v1">
         <response>
            <sak>
               <saksId>010847146</saksId>
               <sakstema>AAP</sakstema>
               <opprettet>2018-07-24T13:43:27.444+02:00</opprettet>
               <behandlingskjede>
                  <behandlingskjedeId>00T53Ao</behandlingskjedeId>
                  <behandlingskjedetype>ad0001</behandlingskjedetype>
                  <start>2018-07-24T13:43:27.444+02:00</start>
                  <slutt>2018-09-19T04:00:01.177+02:00</slutt>
                  <sluttNAVtid>2018-09-19T04:00:00.547+02:00</sluttNAVtid>
                  <sisteBehandlingREF>1000HZUY9</sisteBehandlingREF>
                  <sisteBehandlingstype>ae0001</sisteBehandlingstype>
                  <sisteBehandlingsstegREF>af0002</sisteBehandlingsstegREF>
                  <sisteBehandlingsstegtype>af0002</sisteBehandlingsstegtype>
                  <behandlingsListeRef>1000HZUY9</behandlingsListeRef>
                  <sisteBehandlingsoppdatering>2018-09-19T04:00:00.547+02:00</sisteBehandlingsoppdatering>
                  <sisteBehandlingsstatus>avbrutt</sisteBehandlingsstatus>
                  <sisteBehandlingAvslutningsstatus>avbrutt-pga-tidsfrist</sisteBehandlingAvslutningsstatus>
               </behandlingskjede>
            </sak>
            <sak>
               <saksId>010847171</saksId>
               <sakstema>SYM</sakstema>
               <opprettet>2018-08-08T00:00:00.625+02:00</opprettet>
               <behandlingskjede>
                  <behandlingskjedeId>00T63LX</behandlingskjedeId>
                  <behandlingskjedetype>ukjent</behandlingskjedetype>
                  <start>2018-11-19T12:33:51.841+01:00</start>
                  <slutt>2018-11-19T12:33:59.389+01:00</slutt>
                  <sluttNAVtid>2018-11-19T12:33:51.841+01:00</sluttNAVtid>
                  <sisteBehandlingREF>1000i0HQ4</sisteBehandlingREF>
                  <sisteBehandlingstype>ae0105</sisteBehandlingstype>
                  <behandlingsListeRef>1000i0HQ4</behandlingsListeRef>
                  <sisteBehandlingsoppdatering>2018-11-19T12:33:51.841+01:00</sisteBehandlingsoppdatering>
                  <sisteBehandlingsstatus>avsluttet</sisteBehandlingsstatus>
                  <sisteBehandlingAvslutningsstatus>ok</sisteBehandlingAvslutningsstatus>
               </behandlingskjede>
               <behandlingskjede>
                  <behandlingskjedeId>00T63ND</behandlingskjedeId>
                  <behandlingskjedetype>ukjent</behandlingskjedetype>
                  <start>2018-11-19T15:35:48.062+01:00</start>
                  <slutt>2018-11-19T15:35:49.807+01:00</slutt>
                  <sluttNAVtid>2018-11-19T15:35:48.063+01:00</sluttNAVtid>
                  <sisteBehandlingREF>1000i0HQA</sisteBehandlingREF>
                  <sisteBehandlingstype>ae0105</sisteBehandlingstype>
                  <behandlingsListeRef>1000i0HQA</behandlingsListeRef>
                  <sisteBehandlingsoppdatering>2018-11-19T15:35:48.063+01:00</sisteBehandlingsoppdatering>
                  <sisteBehandlingsstatus>avsluttet</sisteBehandlingsstatus>
                  <sisteBehandlingAvslutningsstatus>ok</sisteBehandlingAvslutningsstatus>
               </behandlingskjede>
            </sak>
         </response>
      </ns2:finnSakOgBehandlingskjedeListeResponse>
   </soap:Body>
</soap:Envelope>
""".trimIndent()

private fun faultXml(fault: String) = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body>
        <soap:Fault>
            <faultcode xmlns:ns1="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">soap:Server</faultcode>
            <faultstring>$fault</faultstring>
        </soap:Fault>
    </soap:Body>
</soap:Envelope>
""".trimIndent()
