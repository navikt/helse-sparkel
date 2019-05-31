package no.nav.helse.domene.aiy.organisasjon

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
import no.nav.helse.oppslag.organisasjon.OrganisasjonClient
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.Virksomhet
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonResponse
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrganisasjonComponentTest {
    @Test
    fun `skal returnere organiasasjon`() {
        val organisasjonV5 = mockk<OrganisasjonV5>()

        val orgNr = "889640782"

        every {
            organisasjonV5.hentOrganisasjon(match {
                it.orgnummer == orgNr
            })
        } returns HentOrganisasjonResponse().apply {
            organisasjon = Virksomhet().apply {
                orgnummer = orgNr
                navn = UstrukturertNavn().apply {
                    with(navnelinje) {
                        add("NAV")
                        add("AVD SANNERGATA 2")
                    }
                }
            }
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                organisasjonService = OrganisasjonService(OrganisasjonClient(organisasjonV5)))}) {
            handleRequest(HttpMethod.Get, "/api/organisasjon/${orgNr}") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertJsonEquals(JSONObject(expectedJson), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `skal svare med feil ved feil`() {
        val organisasjonV5 = mockk<OrganisasjonV5>()

        val orgNr = "889640782"

        every {
            organisasjonV5.hentNoekkelinfoOrganisasjon(match {
                it.orgnummer == orgNr
            })
        } throws (Exception("SOAP fault"))

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                organisasjonService = OrganisasjonService(OrganisasjonClient(organisasjonV5)))}) {
            handleRequest(HttpMethod.Get, "/api/organisasjon/${orgNr}") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
                assertJsonEquals(JSONObject(expectedJson_fault), JSONObject(response.content))
            }
        }
    }
}

private val expectedJson = """
{
    "orgnummer": "889640782",
    "navn": "NAV, AVD SANNERGATA 2",
    "type": "Virksomhet"
}
""".trimIndent()

private val expectedJson_fault = """
{
    "feilmelding":"Unknown error"
}
""".trimIndent()
