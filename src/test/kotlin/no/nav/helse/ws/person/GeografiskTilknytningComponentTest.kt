package no.nav.helse.ws.person

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
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.person.client.PersonClient
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bydel
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kommune
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Land
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse
import org.json.JSONObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GeografiskTilknytningComponentTest {

    @Test
    fun `Geografisk Tilknytning på en person med kode 6`() {
        val aktørId = AktørId("1234567891011")

        val personV3 = mockk<PersonV3>()
        every {
            personV3.hentGeografiskTilknytning(match {
                (it.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } returns HentGeografiskTilknytningResponse().apply {
            aktoer = AktoerId().apply {
                aktoerId = aktørId.aktor
            }
            navn = Personnavn().apply {
                etternavn = "DAME"
                mellomnavn = "SKJERMET"
                sammensattNavn = "DAME SKJERMET"
            }
            diskresjonskode = Diskresjonskoder().apply {
                value = "SPSF"
            }
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                personService = PersonService(PersonClient(personV3))
        )}) {
            handleRequest(HttpMethod.Get, "/api/person/${aktørId.aktor}/geografisk-tilknytning") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                Assertions.assertEquals(HttpStatusCode.Forbidden.value, response.status()?.value)
                assertJsonEquals(JSONObject(expected_geografisk_tilknytning_kode_6_response), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `Geografisk Tilknytning på en person med kode 7`() {
        val aktørId = AktørId("1234567891011")

        val personV3 = mockk<PersonV3>()
        every {
            personV3.hentGeografiskTilknytning(match {
                (it.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } returns HentGeografiskTilknytningResponse().apply {
            aktoer = AktoerId().apply {
                aktoerId = aktørId.aktor
            }
            navn = Personnavn().apply {
                etternavn = "DORULL"
                mellomnavn = "DORULL STOR"
                sammensattNavn = "STOR"
            }
            diskresjonskode = Diskresjonskoder().apply {
                value = "SPFO"
            }
            geografiskTilknytning = Land().apply {
                geografiskTilknytning = "030155"
            }
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                personService = PersonService(PersonClient(personV3))
        )}) {
            handleRequest(HttpMethod.Get, "/api/person/${aktørId.aktor}/geografisk-tilknytning") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK.value, response.status()?.value)
                assertJsonEquals(JSONObject(expected_geografisk_tilknytning_response("Land", "030155")), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `Geografisk Tilknytning på en person registrert på land`() {
        val aktørId = AktørId("1234567891011")

        val personV3 = mockk<PersonV3>()
        every {
            personV3.hentGeografiskTilknytning(match {
                (it.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } returns HentGeografiskTilknytningResponse().apply {
            aktoer = AktoerId().apply {
                aktoerId = aktørId.aktor
            }
            navn = Personnavn().apply {
                etternavn = "LOLNES"
                mellomnavn = "PIKENES"
                fornavn = "JENNY"
                sammensattNavn = "LOLNES JENNY PIKENES"
            }
            geografiskTilknytning = Land().apply {
                geografiskTilknytning = "030103"
            }
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                personService = PersonService(PersonClient(personV3))
        )}) {
            handleRequest(HttpMethod.Get, "/api/person/${aktørId.aktor}/geografisk-tilknytning") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK.value, response.status()?.value)
                assertJsonEquals(JSONObject(expected_geografisk_tilknytning_response("Land", "030103")), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `Geografisk Tilknytning på en person registrert på kommune`() {
        val aktørId = AktørId("1234567891011")

        val personV3 = mockk<PersonV3>()
        every {
            personV3.hentGeografiskTilknytning(match {
                (it.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } returns HentGeografiskTilknytningResponse().apply {
            aktoer = AktoerId().apply {
                aktoerId = aktørId.aktor
            }
            navn = Personnavn().apply {
                etternavn = "LOLNES"
                mellomnavn = "PIKENES"
                fornavn = "JENNY"
                sammensattNavn = "LOLNES JENNY PIKENES"
            }
            geografiskTilknytning = Kommune().apply {
                geografiskTilknytning = "030104"
            }
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                personService = PersonService(PersonClient(personV3))
        )}) {
            handleRequest(HttpMethod.Get, "/api/person/${aktørId.aktor}/geografisk-tilknytning") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK.value, response.status()?.value)
                assertJsonEquals(JSONObject(expected_geografisk_tilknytning_response("Kommune", "030104")), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `Geografisk Tilknytning på en person registrert på bydel`() {
        val aktørId = AktørId("1234567891011")

        val personV3 = mockk<PersonV3>()
        every {
            personV3.hentGeografiskTilknytning(match {
                (it.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } returns HentGeografiskTilknytningResponse().apply {
            aktoer = AktoerId().apply {
                aktoerId = aktørId.aktor
            }
            navn = Personnavn().apply {
                etternavn = "LOLNES"
                mellomnavn = "PIKENES"
                fornavn = "JENNY"
                sammensattNavn = "LOLNES JENNY PIKENES"
            }
            geografiskTilknytning = Bydel().apply {
                geografiskTilknytning = "030105"
            }
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                personService = PersonService(PersonClient(personV3))
        )}) {
            handleRequest(HttpMethod.Get, "/api/person/${aktørId.aktor}/geografisk-tilknytning") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK.value, response.status()?.value)
                assertJsonEquals(JSONObject(expected_geografisk_tilknytning_response("Bydel", "030105")), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `Person uten Geografisk Tilnytning`() {
        val aktørId = AktørId("1234567891011")

        val personV3 = mockk<PersonV3>()
        every {
            personV3.hentGeografiskTilknytning(match {
                (it.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } returns HentGeografiskTilknytningResponse().apply {
            aktoer = AktoerId().apply {
                aktoerId = aktørId.aktor
            }
            navn = Personnavn().apply {
                etternavn = "LOLNES"
                mellomnavn = "PIKENES"
                fornavn = "JENNY"
                sammensattNavn = "LOLNES JENNY PIKENES"
            }
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                personService = PersonService(PersonClient(personV3))
        )}) {
            handleRequest(HttpMethod.Get, "/api/person/${aktørId.aktor}/geografisk-tilknytning") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                Assertions.assertEquals(HttpStatusCode.NotFound.value, response.status()?.value)
                assertJsonEquals(JSONObject(expected_geografisk_tilknytning_empty_response), JSONObject(response.content))
            }
        }
    }
}

private val expected_geografisk_tilknytning_kode_6_response = """
{
    "feilmelding": "Ikke tilgang til å se geografisk tilknytning til denne aktøren."
}
""".trimIndent()

private fun expected_geografisk_tilknytning_response(type: String, kode: String) = """
{
    "type": "${type.toUpperCase()}",
    "kode": "$kode"
}
""".trimIndent()

private val expected_geografisk_tilknytning_empty_response = """
{
    "feilmelding": "Aktøren har ingen geografisk tilknytning."
}
""".trimIndent()
