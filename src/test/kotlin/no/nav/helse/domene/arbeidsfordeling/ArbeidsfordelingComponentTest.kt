package no.nav.helse.domene.arbeidsfordeling

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.JwtStub
import no.nav.helse.assertJsonEquals
import no.nav.helse.mockedSparkel
import no.nav.helse.oppslag.person.PersonClient
import no.nav.helse.domene.person.PersonService
import no.nav.helse.oppslag.arbeidsfordeling.ArbeidsfordelingClient
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Enhetsstatus
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Organisasjonsenhet
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeResponse
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bydel
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse
import org.json.JSONObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ArbeidsfordelingComponentTest {

    @Test
    fun `returnerer feil når tema ikke er satt`() {
        val aktør = "11987654321"
        val expectedJson = """
            {
                "feilmelding": "Requesten må inneholde query parameter 'tema'"
            }
        """.trimIndent()

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvspa")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider()
        )}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsfordeling/behandlende-enhet/$aktør") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertJsonEquals(JSONObject(expectedJson), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `returnerer enhet for aktør`() {
        val aktørId = "1831212532200"
        val geografiskOmraadeKode = "030103"
        val tema = "OMS"

        val enhetId = "4432"
        val enhetNavn = "NAV Arbeid og ytelser Follo"

        val expectedJson = """
            {
                "id": "$enhetId",
                "navn": "$enhetNavn"
            }
        """.trimIndent()

        val arbeidsfordelingV1Mock = mockk<ArbeidsfordelingV1>()
        every {
            arbeidsfordelingV1Mock.finnBehandlendeEnhetListe(any())
        } returns FinnBehandlendeEnhetListeResponse().apply {
            behandlendeEnhetListe.add(Organisasjonsenhet().apply {
                this.status = Enhetsstatus.AKTIV
                this.enhetId = enhetId
                this.enhetNavn = enhetNavn
            })
        }

        val personV3Mock = mockk<PersonV3>()
        every {
            personV3Mock.hentGeografiskTilknytning(any())
        } returns HentGeografiskTilknytningResponse().apply {
            diskresjonskode = null
            geografiskTilknytning = Bydel().apply {
                geografiskTilknytning = geografiskOmraadeKode
            }
        }

        val arbeidsfordelingService = ArbeidsfordelingService(
                arbeidsfordelingClient = ArbeidsfordelingClient(arbeidsfordelingV1Mock),
                personService = PersonService(PersonClient(personV3Mock))
        )

        val jwkStub = JwtStub("test issuer")

        val token = jwkStub.createTokenFor("srvspa")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                arbeidsfordelingService = arbeidsfordelingService
        )}) {
            handleRequest(HttpMethod.Get, "/api/arbeidsfordeling/behandlende-enhet/$aktørId?tema=$tema") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(200, response.status()?.value)
                assertJsonEquals(JSONObject(expectedJson), JSONObject(response.content))
            }
        }
    }
}

