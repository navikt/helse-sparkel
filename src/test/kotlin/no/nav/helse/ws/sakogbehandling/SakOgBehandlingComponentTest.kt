package no.nav.helse.ws.sakogbehandling

import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import no.nav.helse.*
import no.nav.helse.common.*
import no.nav.helse.ws.*
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.*
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.finnsakogbehandlingskjedeliste.*
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.sakogbehandling.*
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.*
import org.json.*
import org.junit.jupiter.api.Test
import java.time.*
import kotlin.test.*

class SakOgBehandlingComponentTest {

    @Test
    fun `tom saksliste fra sakogbehandling gir tom respons`() {
        val sakOgBehandlingV1 = mockk<SakOgBehandlingV1>()

        val aktørId = AktørId("123456789101112")

        every {
            sakOgBehandlingV1.finnSakOgBehandlingskjedeListe(match {
                it.aktoerREF == aktørId.aktor
            })
        } returns FinnSakOgBehandlingskjedeListeResponse()

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                sakOgBehandlingService = SakOgBehandlingService(SakOgBehandlingClient(sakOgBehandlingV1))
        )}) {
            handleRequest(HttpMethod.Get, "/api/sakogbehandling/${aktørId.aktor}") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(200, response.status()?.value)
                assertTrue(JSONArray(response.content).isEmpty)
            }
        }
    }

    @Test
    fun `flere saker`() {
        val sakOgBehandlingV1 = mockk<SakOgBehandlingV1>()

        val aktørId = AktørId("123456789101112")

        every {
            sakOgBehandlingV1.finnSakOgBehandlingskjedeListe(match {
                it.aktoerREF == aktørId.aktor
            })
        } returns FinnSakOgBehandlingskjedeListeResponse().apply {
            sak.add(sakUtenBehandlinger("el sako grande"))
            sak.add(sakUtenBehandlinger("el grande sako"))
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                sakOgBehandlingService = SakOgBehandlingService(SakOgBehandlingClient(sakOgBehandlingV1))
        )}) {
            handleRequest(HttpMethod.Get, "/api/sakogbehandling/${aktørId.aktor}") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(200, response.status()?.value)
                assertEquals(JSONArray(response.content).length(), 2)
            }
        }
    }

    @Test
    fun `sak med behandlinger`() {
        val sakOgBehandlingV1 = mockk<SakOgBehandlingV1>()

        val aktørId = AktørId("123456789101112")

        every {
            sakOgBehandlingV1.finnSakOgBehandlingskjedeListe(match {
                it.aktoerREF == aktørId.aktor
            })
        } returns FinnSakOgBehandlingskjedeListeResponse().apply {
            sak.add(sakUtenBehandlinger("el sako grande").apply { behandlingskjede.add(behandling()) })
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                sakOgBehandlingService = SakOgBehandlingService(SakOgBehandlingClient(sakOgBehandlingV1))
        )}) {
            handleRequest(HttpMethod.Get, "/api/sakogbehandling/${aktørId.aktor}") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(200, response.status()?.value)
                println(response.content)
            }
        }
    }

    private fun sakUtenBehandlinger(id: String) =
            no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.finnsakogbehandlingskjedeliste.Sak().apply {
                saksId = id
                sakstema = Sakstemaer().apply { value = "temaet" }
                opprettet = LocalDate.parse("2019-02-26").toXmlGregorianCalendar()
            }

    private fun behandling() =
        Behandlingskjede().apply {
            sisteBehandlingsstatus = Behandlingsstatuser().apply { value = "behandlingsstatusen" }
            slutt = LocalDate.parse("2019-02-26").toXmlGregorianCalendar()
        }

}
