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
import no.nav.helse.common.toXmlGregorianCalendar
import no.nav.helse.mockedSparkel
import no.nav.helse.ws.AktørId
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.feil.PersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonComponentTest {

    @Test
    fun `skal svare med person`() {
        val aktørId = AktørId("1234567891011")

        val personV3 = mockk<PersonV3>()
        every {
            personV3.hentPerson(match {
                (it.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } returns HentPersonResponse().apply {
            person = Person().apply {
                aktoer = AktoerId().apply {
                    aktoerId = aktørId.aktor
                }
                personnavn = Personnavn().apply {
                    etternavn = "LOLNES"
                    mellomnavn = "PIKENES"
                    fornavn = "JENNY"
                }
                kjoenn = Kjoenn().apply {
                    kjoenn = Kjoennstyper().apply {
                        value = "K"
                    }
                }
                bostedsadresse = Bostedsadresse().apply {
                    strukturertAdresse = Gateadresse().apply {
                        landkode = Landkoder().apply {
                            value = "NOR"
                        }
                    }
                }
                foedselsdato = Foedselsdato().apply {
                    foedselsdato = LocalDate.parse("1984-07-08").toXmlGregorianCalendar()
                }
            }
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                personService = PersonService(PersonClient(personV3))
        )}) {
            handleRequest(HttpMethod.Get, "/api/person/${aktørId.aktor}") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertJsonEquals(JSONObject(expected_person_response), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `skal svare med person uten bostedadresse`() {
        val aktørId = AktørId("1234567891011")

        val personV3 = mockk<PersonV3>()
        every {
            personV3.hentPerson(match {
                (it.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } returns HentPersonResponse().apply {
            person = Person().apply {
                aktoer = AktoerId().apply {
                    aktoerId = aktørId.aktor
                }
                personnavn = Personnavn().apply {
                    etternavn = "LOLNES"
                    mellomnavn = "PIKENES"
                    fornavn = "JENNY"
                }
                kjoenn = Kjoenn().apply {
                    kjoenn = Kjoennstyper().apply {
                        value = "K"
                    }
                }
                diskresjonskode = Diskresjonskoder().apply {
                    value = "UFB"
                }
                foedselsdato = Foedselsdato().apply {
                    foedselsdato = LocalDate.parse("1984-07-08").toXmlGregorianCalendar()
                }
                statsborgerskap = Statsborgerskap().apply {
                    land = Landkoder().apply {
                        value = "SWE"
                    }
                }
                personstatus = Personstatus().apply {
                    personstatus = Personstatuser().apply {
                        value = "UTVA"
                    }
                }
            }
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                personService = PersonService(PersonClient(personV3))
        )}) {
            handleRequest(HttpMethod.Get, "/api/person/${aktørId.aktor}") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertJsonEquals(JSONObject(expected_person_uten_fast_bopel_response), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `skal svare med feil når personoppslag gir feil`() {
        val aktørId = AktørId("1234567891011")

        val personV3 = mockk<PersonV3>()
        every {
            personV3.hentPerson(match {
                (it.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } throws(HentPersonPersonIkkeFunnet("Person ikke funnet", PersonIkkeFunnet()))

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                personService = PersonService(PersonClient(personV3))
        )}) {
            handleRequest(HttpMethod.Get, "/api/person/${aktørId.aktor}") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
                assertJsonEquals(JSONObject(expected_not_found_response), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `skal svare med geografisk tilknytning`() {
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
                etternavn = "BLYANT"
                mellomnavn = "SMEKKER"
                sammensattNavn = "BLYANT SMEKKER"
            }
            geografiskTilknytning = Bydel().apply {
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
                assertEquals(HttpStatusCode.OK.value, response.status()?.value)
                assertJsonEquals(JSONObject(expected_geografisk_tilknytning_response), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `skal svare med feil når geografisk tilknytningoppslag gir feil`() {
        val aktørId = AktørId("1234567891011")

        val personV3 = mockk<PersonV3>()
        every {
            personV3.hentGeografiskTilknytning(match {
                (it.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } throws(HentGeografiskTilknytningPersonIkkeFunnet("Person ikke funnet", PersonIkkeFunnet()))

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
                assertEquals(HttpStatusCode.NotFound, response.status())
                assertJsonEquals(JSONObject(expected_not_found_response), JSONObject(response.content))
            }
        }
    }
}

private val expected_person_response = """
{
    "aktørId": "1234567891011",
    "fdato": "1984-07-08",
    "etternavn": "LOLNES",
    "mellomnavn": "PIKENES",
    "fornavn": "JENNY",
    "kjønn": "KVINNE",
    "bostedsland": "NOR"
}
""".trimIndent()

private val expected_person_uten_fast_bopel_response = """
{
    "aktørId": "1234567891011",
    "fdato": "1984-07-08",
    "etternavn": "LOLNES",
    "mellomnavn": "PIKENES",
    "fornavn": "JENNY",
    "kjønn": "KVINNE",
    "diskresjonskode": "UFB",
    "statsborgerskap": "SWE",
    "status": "UTVA"
}
""".trimIndent()

private val expected_geografisk_tilknytning_response = """
{
    "kode": "030103",
    "type": "BYDEL"
}
""".trimIndent()

private val expected_not_found_response = """
{
    "feilmelding": "Resource not found"
}
""".trimIndent()
