package no.nav.helse.ws.sakogbehandling

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.prometheus.client.CollectorRegistry
import no.nav.helse.Environment
import no.nav.helse.JwtStub
import no.nav.helse.assertJsonEquals
import no.nav.helse.mockedSparkel
import no.nav.helse.sts.StsRestClient
import no.nav.helse.ws.WsClients
import no.nav.helse.ws.samlAssertionResponse
import no.nav.helse.ws.sts.stsClient
import no.nav.helse.ws.stsStub
import no.nav.helse.ws.withCallId
import no.nav.helse.ws.withSamlAssertion
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SakOgBehandlingComponentTest {

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

    @AfterEach
    fun `clear prometheus registry`() {
        CollectorRegistry.defaultRegistry.clear()
    }

    @Test
    fun `that response is json`() {
        val jwtStub = JwtStub("test issuer")
        val token = jwtStub.createTokenFor("srvspinne")

        WireMock.stubFor(stsStub("stsUsername", "stsPassword")
                .willReturn(samlAssertionResponse("testusername", "theIssuer", "CN=B27 Issuing CA Intern, DC=preprod, DC=local",
                        "digestValue", "signatureValue", "certificateValue")))

        WireMock.stubFor(finnSakOgBehandlingskjedeListeStub("1242536513046")
                .withSamlAssertion("testusername", "theIssuer", "CN=B27 Issuing CA Intern, DC=preprod, DC=local",
                        "digestValue", "signatureValue", "certificateValue")
                .withCallId("Sett inn call id her")
                .willReturn(WireMock.okXml(finnSakOgBehandlingskjedeListe_response)))

        val env = Environment(mapOf(
                "SECURITY_TOKEN_SERVICE_URL" to server.baseUrl().plus("/sts"),
                "SECURITY_TOKEN_SERVICE_REST_URL" to server.baseUrl().plus("/sts"),
                "SECURITY_TOKEN_SERVICE_USERNAME" to "stsUsername",
                "SECURITY_TOKEN_SERVICE_PASSWORD" to "stsPassword",
                "SAK_OG_BEHANDLING_ENDPOINTURL" to server.baseUrl().plus("/sakogbehandling"),
                "AKTORREGISTER_URL" to server.baseUrl().plus("/aktor"),
                "ORGANISASJON_ENDPOINTURL" to server.baseUrl().plus("/organisasjon"),
                "PERSON_ENDPOINTURL" to server.baseUrl().plus("/person"),
                "ARBEIDSFORDELING_ENDPOINTURL" to server.baseUrl().plus("/arbeidsfordeling"),
                "INNTEKT_ENDPOINTURL" to server.baseUrl().plus("/inntekt"),
                "AAREG_ENDPOINTURL" to server.baseUrl().plus("/aareg"),
                "HENT_SYKEPENGER_ENDPOINTURL" to server.baseUrl().plus("/sykepenger"),
                "MELDEKORT_UTBETALINGSGRUNNLAG_ENDPOINTURL" to server.baseUrl().plus("/meldekort"),
                "JWT_ISSUER" to "test issuer",
                "ALLOW_INSECURE_SOAP_REQUESTS" to "true"
        ))

        val stsClientWs = stsClient(env.securityTokenServiceEndpointUrl,
                env.securityTokenUsername to env.securityTokenPassword)
        val stsClientRest = StsRestClient(
                env.stsRestUrl, env.securityTokenUsername, env.securityTokenPassword)

        val wsClients = WsClients(stsClientWs, stsClientRest, env.allowInsecureSoapRequests)

        withTestApplication({mockedSparkel(
                jwtIssuer = env.jwtIssuer,
                jwkProvider = jwtStub.stubbedJwkProvider(),
                sakOgBehandlingService = SakOgBehandlingService(wsClients.sakOgBehandling(env.sakOgBehandlingEndpointUrl))
        )}) {
            handleRequest(HttpMethod.Post, "api/sakogbehandling") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
                setBody("{\"aktorId\": \"1242536513046\"}")
            }.apply {
                Assertions.assertEquals(200, response.status()?.value)
                assertJsonEquals(JSONObject(expectedJson), JSONObject(response.content))
            }
        }
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

private val expectedJson = """
{
  "sak": [
    {
      "opprettet": "2018-07-24T13:43:27.444+02:00",
      "saksId": "010847146",
      "behandlingskjede": [
        {
          "behandlingskjedeId": "00T53Ao",
          "sisteBehandlingsstegtype": {
            "kodeverksRef": "http://nav.no/kodeverk/Behandlingsstegtyper",
            "value": "af0002"
          },
          "start": "2018-07-24T13:43:27.444+02:00",
          "sisteBehandlingsstegREF": "af0002",
          "sisteBehandlingAvslutningsstatus": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Avslutningsstatuser",
            "value": "avbrutt-pga-tidsfrist"
          },
          "behandlingsListeRef": [
            "1000HZUY9"
          ],
          "sisteBehandlingREF": "1000HZUY9",
          "sluttNAVtid": "2018-09-19T04:00:00.547+02:00",
          "behandlingskjedetype": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Behandlingskjedetype",
            "value": "ad0001"
          },
          "slutt": "2018-09-19T04:00:01.177+02:00",
          "sisteBehandlingstype": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Behandlingstyper",
            "value": "ae0001"
          },
          "sisteBehandlingsstatus": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Behandlingsstatuser",
            "value": "avbrutt"
          },
          "sisteBehandlingsoppdatering": "2018-09-19T04:00:00.547+02:00"
        }
      ],
      "sakstema": {
        "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Sakstemaer",
        "value": "AAP"
      }
    },
    {
      "opprettet": "2018-08-08T00:00:00.625+02:00",
      "saksId": "010847171",
      "behandlingskjede": [
        {
          "behandlingskjedeId": "00T63LX",
          "sisteBehandlingAvslutningsstatus": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Avslutningsstatuser",
            "value": "ok"
          },
          "behandlingsListeRef": [
            "1000i0HQ4"
          ],
          "sisteBehandlingREF": "1000i0HQ4",
          "start": "2018-11-19T12:33:51.841+01:00",
          "sluttNAVtid": "2018-11-19T12:33:51.841+01:00",
          "behandlingskjedetype": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Behandlingskjedetype",
            "value": "ukjent"
          },
          "slutt": "2018-11-19T12:33:59.389+01:00",
          "sisteBehandlingstype": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Behandlingstyper",
            "value": "ae0105"
          },
          "sisteBehandlingsstatus": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Behandlingsstatuser",
            "value": "avsluttet"
          },
          "sisteBehandlingsoppdatering": "2018-11-19T12:33:51.841+01:00"
        },
        {
          "behandlingskjedeId": "00T63ND",
          "sisteBehandlingAvslutningsstatus": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Avslutningsstatuser",
            "value": "ok"
          },
          "behandlingsListeRef": [
            "1000i0HQA"
          ],
          "sisteBehandlingREF": "1000i0HQA",
          "start": "2018-11-19T15:35:48.062+01:00",
          "sluttNAVtid": "2018-11-19T15:35:48.063+01:00",
          "behandlingskjedetype": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Behandlingskjedetype",
            "value": "ukjent"
          },
          "slutt": "2018-11-19T15:35:49.807+01:00",
          "sisteBehandlingstype": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Behandlingstyper",
            "value": "ae0105"
          },
          "sisteBehandlingsstatus": {
            "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Behandlingsstatuser",
            "value": "avsluttet"
          },
          "sisteBehandlingsoppdatering": "2018-11-19T15:35:48.063+01:00"
        }
      ],
      "sakstema": {
        "kodeverksRef": "http://nav.no/kodeverk/Kodeverk/Sakstemaer",
        "value": "SYM"
      }
    }
  ]
}
""".trimIndent()
