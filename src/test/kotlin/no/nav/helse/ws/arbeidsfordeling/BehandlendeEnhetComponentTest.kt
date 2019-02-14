package no.nav.helse.ws.arbeidsfordeling

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.helse.assertJsonEquals
import no.nav.helse.bootstrapComponentTest
import no.nav.helse.sparkel
import no.nav.helse.ws.person.GeografiskTilknytningMocks
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals

class BehandlendeEnhetComponentTest {

    private val log = LoggerFactory.getLogger("BehandlendeEnhetComponentTest")


    companion object {
        private val bootstrap = bootstrapComponentTest()
        private val token = bootstrap.jwkStub.createTokenFor("srvpleiepenger-opp")

        @BeforeAll
        @JvmStatic
        fun start() {
            bootstrap.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            bootstrap.stop()
        }
    }

    @BeforeEach
    @AfterEach
    fun `reset`() {
        bootstrap.reset()
    }

    @Test
    fun `Finn enhet for hovedaktør uten kode 6 uten noen medaktører`() {
        val aktoerId = "1831212532200"
        val geografiskOmraadeKode = "030103"
        val tema = "OMS"

        // Mocker først request gjort mot geografisk tilknytning
        val geografiskTilknytningResponses = GeografiskTilknytningMocks.medGeografiskTilknytningResponses(
                aktoerId = aktoerId,
                geografiskOmraadeType = "Bydel",
                geografiskOmraadeKode = geografiskOmraadeKode
        )

        GeografiskTilknytningMocks.mock(
                aktoerId = aktoerId,
                xml = geografiskTilknytningResponses.registerXmlResponse
        )

        // Mocker oppslag på enhet
        val behandlendeEnhetResponses = BehandlendeEnhetMocks.enAktivEnhetResponses(
                enhetId = "4432",
                enhetNavn = "NAV Arbeid og ytelser Follo"
        )
        BehandlendeEnhetMocks.mockMedGeografiskTilknytning(
                geografiskTilknytning = geografiskOmraadeKode,
                tema = tema,
                xml = behandlendeEnhetResponses.registerXmlResponse
        )

        // Sender request til sparkel
        requestSparkelAndAssertResponse(
                aktoerId = aktoerId,
                expectedResponse = behandlendeEnhetResponses.sparkelJsonResponse,
                tema = tema
        )
    }

    @Test
    fun `Finn enhet for hovedaktør med kode 6 uten noen medaktører`() {
        val aktoerId = "1831212532201"
        val tema = "OMS"

        // Mocker først request gjort mot geografisk tilknytning
        val geografiskTilknytningResponses = GeografiskTilknytningMocks.medDiskresjonsKode6Responses(
                aktoerId = aktoerId
        )

        GeografiskTilknytningMocks.mock(
                aktoerId = aktoerId,
                xml = geografiskTilknytningResponses.registerXmlResponse
        )

        // Mocker oppslag på enhet
        val behandlendeEnhetResponses = BehandlendeEnhetMocks.enAktivEnhetResponses(
                enhetId = "2103",
                enhetNavn = "NAV Viken"
        )
        BehandlendeEnhetMocks.mockKode6(
                tema = tema,
                xml = behandlendeEnhetResponses.registerXmlResponse
        )

        // Sender request til sparkel
        requestSparkelAndAssertResponse(
                aktoerId = aktoerId,
                expectedResponse = behandlendeEnhetResponses.sparkelJsonResponse,
                tema = tema
        )
    }

    @Test
    fun `Finn enhet for hovedaktør med kode 7 uten noen medaktører`() {
        val aktoerId = "1831212532202"
        val geografiskOmraadeKode = "030103"

        val tema = "OMS"

        // Mocker først request gjort mot geografisk tilknytning
        val geografiskTilknytningResponses = GeografiskTilknytningMocks.medDiskresjonsKode7Responses(
                aktoerId = aktoerId,
                geografiskOmraadeType = "Bydel",
                geografiskOmraadeKode = geografiskOmraadeKode
        )

        GeografiskTilknytningMocks.mock(
                aktoerId = aktoerId,
                xml = geografiskTilknytningResponses.registerXmlResponse
        )

        // Mocker oppslag på enhet
        val behandlendeEnhetResponses = BehandlendeEnhetMocks.enAktivEnhetResponses(
                enhetId = "4432",
                enhetNavn = "NAV Arbeid og ytelser Follo"
        )
        BehandlendeEnhetMocks.mockKode7(
                tema = tema,
                geografiskTilknytning = geografiskOmraadeKode,
                xml = behandlendeEnhetResponses.registerXmlResponse
        )

        // Sender request til sparkel
        requestSparkelAndAssertResponse(
                aktoerId = aktoerId,
                expectedResponse = behandlendeEnhetResponses.sparkelJsonResponse,
                tema = tema
        )
    }

    @Test
    fun `Finn enhet for hovedaktør uten kode 6 og medaktør med kode 6`() {
        val aktoerIdHovedAktoer = "1831212532203"
        val aktoerIdMedAktoerIkkeKode6 = "1831212532204"
        val aktoerIdMedAktoerKode6 = "1831212532205"

        val tema = "OMS"

        // Hovedsøker
        val geografiskTilknytningResponsesHovedAktoer = GeografiskTilknytningMocks.medGeografiskTilknytningResponses(
                aktoerId = aktoerIdHovedAktoer,
                geografiskOmraadeType = "Bydel",
                geografiskOmraadeKode = "030103"
        )

        GeografiskTilknytningMocks.mock(
                aktoerId = aktoerIdHovedAktoer,
                xml = geografiskTilknytningResponsesHovedAktoer.registerXmlResponse
        )

        val behandlendeEnhetResponsesHovedAktoer = BehandlendeEnhetMocks.enAktivEnhetResponses(
                enhetId = "4432",
                enhetNavn = "NAV Arbeid og ytelser Follo"
        )
        BehandlendeEnhetMocks.mockMedGeografiskTilknytning(
                tema = tema,
                geografiskTilknytning = "030103",
                xml = behandlendeEnhetResponsesHovedAktoer.registerXmlResponse
        )

        // Medaktør som ikke er kode 6
        val geografiskTilknytningResponseMedAktoerIkkeKode6 = GeografiskTilknytningMocks.medDiskresjonsKode7Responses(
                aktoerId = aktoerIdMedAktoerIkkeKode6,
                geografiskOmraadeType = "Bydel",
                geografiskOmraadeKode = "030104"
        )

        GeografiskTilknytningMocks.mock(
                aktoerId = aktoerIdMedAktoerIkkeKode6,
                xml = geografiskTilknytningResponseMedAktoerIkkeKode6.registerXmlResponse
        )

        val behandlendeEnhetResponsesMedAktoerIkkeKode6 = BehandlendeEnhetMocks.enAktivEnhetResponses(
                enhetId = "4434",
                enhetNavn = "NAV Arbeid og ytelser Elverum"
        )

        BehandlendeEnhetMocks.mockKode7(
                tema = tema,
                geografiskTilknytning = "030103",
                xml = behandlendeEnhetResponsesMedAktoerIkkeKode6.registerXmlResponse
        )
        // Medaktør som har kode 6
        val geografiskTilknytningResponseMedAktoerKode6 = GeografiskTilknytningMocks.medDiskresjonsKode6Responses(
                aktoerId = aktoerIdMedAktoerKode6
        )

        GeografiskTilknytningMocks.mock(
                aktoerId = aktoerIdMedAktoerKode6,
                xml = geografiskTilknytningResponseMedAktoerKode6.registerXmlResponse
        )

        val behandlendeEnhetResponsesMedAktoerKode6 = BehandlendeEnhetMocks.enAktivEnhetResponses(
                enhetId = "2103",
                enhetNavn = "NAV Viken"
        )

        BehandlendeEnhetMocks.mockKode6(
                tema = tema,
                xml = behandlendeEnhetResponsesMedAktoerKode6.registerXmlResponse
        )


        // Sender request til sparkel
        // Tester at vi har fått responsen fra aktøren som har kode 6
        requestSparkelAndAssertResponse(
                aktoerId = aktoerIdHovedAktoer,
                medAktoerIder = listOf(aktoerIdMedAktoerIkkeKode6, aktoerIdMedAktoerKode6),
                expectedResponse = behandlendeEnhetResponsesMedAktoerKode6.sparkelJsonResponse,
                tema = tema
        )
    }

    @Test
    fun `Finn enhet for hovedaktør uten kode 6 og medaktør uten kode 6`() {
        val aktoerIdHovedAktoer = "1831212532206"
        val aktoerIdMedAktoer = "1831212532207"

        val tema = "OMS"

        // Hovedaktør
        val geografiskTilknytningResponsesHovedAktoer = GeografiskTilknytningMocks.medGeografiskTilknytningResponses(
                aktoerId = aktoerIdHovedAktoer,
                geografiskOmraadeType = "Bydel",
                geografiskOmraadeKode = "01336"
        )

        GeografiskTilknytningMocks.mock(
                aktoerId = aktoerIdHovedAktoer,
                xml = geografiskTilknytningResponsesHovedAktoer.registerXmlResponse
        )

        val behandlendeEnhetResponsesHovedAktoer = BehandlendeEnhetMocks.enAktivEnhetResponses(
                enhetId = "4432",
                enhetNavn = "NAV Arbeid og ytelser Follo"
        )
        BehandlendeEnhetMocks.mockMedGeografiskTilknytning(
                geografiskTilknytning = "01336",
                tema = tema,
                xml = behandlendeEnhetResponsesHovedAktoer.registerXmlResponse
        )

        // Medaktør
        val geografiskTilknytningResponsesMedAktoer = GeografiskTilknytningMocks.medGeografiskTilknytningResponses(
                aktoerId = aktoerIdMedAktoer,
                geografiskOmraadeType = "Land",
                geografiskOmraadeKode = "SWE"
        )

        GeografiskTilknytningMocks.mock(
                aktoerId = aktoerIdMedAktoer,
                xml = geografiskTilknytningResponsesMedAktoer.registerXmlResponse
        )

        val behandlendeEnhetResponsesMedAktoer = BehandlendeEnhetMocks.enAktivEnhetResponses(
                enhetId = "4433",
                enhetNavn = "NAV Arbeid og ytelser Elverum"
        )
        BehandlendeEnhetMocks.mockMedGeografiskTilknytning(
                geografiskTilknytning = "SWE",
                tema = tema,
                xml = behandlendeEnhetResponsesMedAktoer.registerXmlResponse
        )

        // Sender request til sparkel
        // Sjekker at vi får responsen fra hovedsøker
        requestSparkelAndAssertResponse(
                aktoerId = aktoerIdHovedAktoer,
                medAktoerIder = listOf(aktoerIdMedAktoer),
                expectedResponse = behandlendeEnhetResponsesHovedAktoer.sparkelJsonResponse,
                tema = tema
        )
    }

    @Test
    fun `Finn enhet for hovedaktør uten kode 6 og uten geografisk tilknytning`() {
        val aktoerId = "1831212532208"
        val tema = "OMS"

        // Mocker først request gjort mot geografisk tilknytning
        val geografiskTilknytningResponses = GeografiskTilknytningMocks.utenGeografiskTilknytningEllerDiskresjonskode(
                aktoerId = aktoerId
        )

        GeografiskTilknytningMocks.mock(
                aktoerId = aktoerId,
                xml = geografiskTilknytningResponses.registerXmlResponse
        )

        // Mocker oppslag på enhet
        val behandlendeEnhetResponses = BehandlendeEnhetMocks.enAktivEnhetResponses(
                enhetId = "4432",
                enhetNavn = "NAV Arbeid og ytelser Follo"
        )
        BehandlendeEnhetMocks.mockBareTema(
                tema = tema,
                xml = behandlendeEnhetResponses.registerXmlResponse
        )

        // Sender request til sparkel
        requestSparkelAndAssertResponse(
                aktoerId = aktoerId,
                expectedResponse = behandlendeEnhetResponses.sparkelJsonResponse,
                tema = tema
        )
    }

    private fun requestSparkelAndAssertResponse(
            aktoerId: String,
            medAktoerIder : List<String> = listOf(),
            tema : String,
            expectedResponse : String
    ) {
        var queryString= "?tema=$tema"
        medAktoerIder.forEach { it ->
            queryString = queryString.plus("&medAktoerId=$it")
        }
        val url = "/api/arbeidsfordeling/behandlende-enhet/$aktoerId$queryString"
        log.info("URL=$url")

        withTestApplication({sparkel(bootstrap.env, bootstrap.jwkStub.stubbedJwkProvider())}) {
            handleRequest(HttpMethod.Get, url) {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(200, response.status()?.value)
                assertJsonEquals(JSONObject(expectedResponse), JSONObject(response.content))
            }
        }
    }
}