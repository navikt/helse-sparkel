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
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonhistorikkPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.feil.PersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.BostedsadressePeriode
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bydel
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Foedselsdato
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoenn
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoennstyper
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Periode
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonstatusPeriode
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatuser
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Postnummer
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Statsborgerskap
import no.nav.tjeneste.virksomhet.person.v3.informasjon.StatsborgerskapPeriode
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkResponse
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                assertEquals(HttpStatusCode.OK.value, response.status()?.value)
                assertJsonEquals(JSONObject(expected_person_response), JSONObject(response.content))
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
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError.value, response.status()?.value)
                val actualJson = JSONObject(response.content)

                assertTrue(actualJson.has("exception"))
                assertEquals("Person ikke funnet", actualJson.get("feilmelding"))
            }
        }
    }

    @Test
    fun `skal svare med personhistorikk`() {
        val aktørId = AktørId("1234567891011")

        val personV3 = mockk<PersonV3>()
        every {
            personV3.hentPersonhistorikk(match {
                (it.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } returns HentPersonhistorikkResponse().apply {
            aktoer = AktoerId().apply {
                aktoerId = aktørId.aktor
            }
            with(personstatusListe) {
                add(PersonstatusPeriode().apply {
                    periode = Periode().apply {
                        fom = LocalDate.now().minusYears(3).toXmlGregorianCalendar()
                        tom = LocalDate.now().toXmlGregorianCalendar()
                    }
                    personstatus = Personstatuser().apply {
                        value = "BOSA"
                    }
                })
            }
            with(statsborgerskapListe) {
                add(StatsborgerskapPeriode().apply {
                    periode = Periode().apply {
                        fom = LocalDate.now().minusYears(3).toXmlGregorianCalendar()
                        tom = LocalDate.now().toXmlGregorianCalendar()
                    }
                    statsborgerskap = Statsborgerskap().apply {
                        land = Landkoder().apply {
                            value = "NOR"
                        }
                    }
                })
            }
            with(bostedsadressePeriodeListe) {
                add(BostedsadressePeriode().apply {
                    periode = Periode().apply {
                        fom = LocalDate.now().minusYears(3).toXmlGregorianCalendar()
                        tom = LocalDate.now().toXmlGregorianCalendar()
                    }
                    bostedsadresse = Bostedsadresse().apply {
                        strukturertAdresse = Gateadresse().apply {
                            landkode = Landkoder().apply {
                                value = "NOR"
                            }
                            poststed = Postnummer().apply {
                                value = "0557"
                            }
                            kommunenummer = "0301"
                            gatenummer = 16188
                            gatenavn = "SANNERGATA"
                            husnummer = 2
                        }
                    }
                })
            }
        }

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                personService = PersonService(PersonClient(personV3))
        )}) {
            handleRequest(HttpMethod.Get, "/api/person/${aktørId.aktor}/history") {
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                assertEquals(HttpStatusCode.OK.value, response.status()?.value)
                assertJsonEquals(JSONObject(expected_personhistorikk_response), JSONObject(response.content))
            }
        }
    }

    @Test
    fun `skal svare med feil når personhistorikk gir feil`() {
        val aktørId = AktørId("1234567891011")

        val personV3 = mockk<PersonV3>()
        every {
            personV3.hentPersonhistorikk(match {
                (it.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } throws(HentPersonhistorikkPersonIkkeFunnet("Person ikke funnet", PersonIkkeFunnet()))

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                personService = PersonService(PersonClient(personV3))
        )}) {
            handleRequest(HttpMethod.Get, "/api/person/${aktørId.aktor}/history") {
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError.value, response.status()?.value)
                val actualJson = JSONObject(response.content)

                assertTrue(actualJson.has("exception"))
                assertEquals("Person ikke funnet", actualJson.get("feilmelding"))
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
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
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
                addHeader(HttpHeaders.Authorization, "Bearer ${token}")
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError.value, response.status()?.value)
                val actualJson = JSONObject(response.content)

                assertTrue(actualJson.has("exception"))
                assertEquals("Person ikke funnet", actualJson.get("feilmelding"))
            }
        }
    }
}

private val expected_person_response = """
{
    "fdato": "1984-07-08",
    "etternavn": "LOLNES",
    "mellomnavn": "PIKENES",
    "id": {
        "aktor": "1234567891011"
    },
    "fornavn": "JENNY",
    "kjønn": "KVINNE",
    "bostedsland": "NOR"
}
""".trimIndent()

private val expected_personhistorikk_response = """
{
    "bostedsadresser": [{
        "tom": "2019-02-24",
        "fom": "2016-02-24",
        "verdi": "SANNERGATA 2, 0557"
    }],
    "statsborgerskap": [{
        "tom": "2019-02-24",
        "fom": "2016-02-24",
        "verdi": "NOR"
    }],
    "statuser": [{
        "tom": "2019-02-24",
        "fom": "2016-02-24",
        "verdi": "BOSA"
    }],
    "id":{
        "aktor": "1234567891011"
    }
}
""".trimIndent()

private val expected_geografisk_tilknytning_response = """
{
    "kode": "030103",
    "type": "BYDEL"
}
""".trimIndent()
