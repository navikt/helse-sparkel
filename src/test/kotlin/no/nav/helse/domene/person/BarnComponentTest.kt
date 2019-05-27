package no.nav.helse.domene.person

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
import no.nav.helse.domene.AktørId
import no.nav.helse.mockedSparkel
import no.nav.helse.oppslag.person.PersonClient
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import no.nav.tjeneste.virksomhet.person.v3.metadata.Endringstyper
import org.json.JSONObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BarnComponentTest {
    @Test
    fun `en person med fire barn`() {
        val aktørId = AktørId("100000000000")
        val barnEnAktørId = AktørId("100000000001")
        val barnToAktørId = AktørId("100000000002")
        val barnTreAktørId = AktørId("100000000003")
        val barnFireAktørId = AktørId("100000000004")

        val personV3 = mockk<PersonV3>()
        every {
            personV3.hentPerson(match {
                (it.aktoer as AktoerId).aktoerId == aktørId.aktor
            })
        } returns hentFamilierelasjonerResponse()

        every {
            personV3.hentPerson(match {
                (it.aktoer as AktoerId).aktoerId == barnEnAktørId.aktor
            })
        } returns hentPersonResponse(
                medAktørId = barnEnAktørId.aktor,
                medFornavn = "BARN",
                medMellomnavn = "EN",
                medEtternavn = "BARNSEN",
                medFødselsdato = LocalDate.of(2018,5,5),
                medStatus = "BOSA"
        )
        every {
            personV3.hentPerson(match {
                (it.aktoer as AktoerId).aktoerId == barnToAktørId.aktor
            })
        } returns hentPersonResponse(
                medAktørId = barnToAktørId.aktor,
                medFornavn = "BARN",
                medMellomnavn = "TO",
                medEtternavn = "BARNSEN",
                medFødselsdato = LocalDate.of(2017,5,5),
                medStatus = "FØDR"
        )
        every {
            personV3.hentPerson(match {
                (it.aktoer as AktoerId).aktoerId == barnTreAktørId.aktor
            })
        } returns hentPersonResponse(
                medAktørId = barnTreAktørId.aktor,
                medFornavn = "BARN",
                medMellomnavn = "TRE",
                medEtternavn = "BARNSEN",
                medFødselsdato = LocalDate.of(2016,5,5),
                medStatus = "DØD"
        )
        every {
            personV3.hentPerson(match {
                (it.aktoer as AktoerId).aktoerId == barnFireAktørId.aktor
            })
        } returns hentPersonResponse(
                medAktørId = barnFireAktørId.aktor,
                medFornavn = "BARN",
                medMellomnavn = "FIRE",
                medEtternavn = "BARNSEN",
                medFødselsdato = LocalDate.of(2015,5,5),
                medStatus = "DØDD"
        )

        val jwkStub = JwtStub("test issuer")
        val token = jwkStub.createTokenFor("srvpleiepengesokna")

        withTestApplication({mockedSparkel(
                jwtIssuer = "test issuer",
                jwkProvider = jwkStub.stubbedJwkProvider(),
                personService = PersonService(PersonClient(personV3))
        )}) {
            handleRequest(HttpMethod.Get, "/api/person/${aktørId.aktor}/barn") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, response.status())
                assertJsonEquals(JSONObject(forventetSparkelResponsToBarn), JSONObject(response.content))
            }
        }
    }

    private val forventetSparkelResponsToBarn = """
    {
    	"barn": [{
    			"statsborgerskap": "NOR",
    			"fdato": "2018-05-05",
    			"etternavn": "BARNSEN",
    			"mellomnavn": "EN",
    			"aktørId": "100000000001",
    			"fornavn": "BARN",
    			"kjønn": "KVINNE",
    			"status": "BOSA"
    		},
    		{
    			"statsborgerskap": "NOR",
    			"fdato": "2017-05-05",
    			"etternavn": "BARNSEN",
    			"mellomnavn": "TO",
    			"aktørId": "100000000002",
    			"fornavn": "BARN",
    			"kjønn": "KVINNE",
    			"status": "FØDR"
    		},
    		{
    			"statsborgerskap": "NOR",
    			"fdato": "2016-05-05",
    			"etternavn": "BARNSEN",
    			"mellomnavn": "TRE",
    			"aktørId": "100000000003",
    			"fornavn": "BARN",
    			"kjønn": "KVINNE",
    			"status": "DØD"
    		},
    		{
    			"statsborgerskap": "NOR",
    			"fdato": "2015-05-05",
    			"etternavn": "BARNSEN",
    			"mellomnavn": "FIRE",
    			"aktørId": "100000000004",
    			"fornavn": "BARN",
    			"kjønn": "KVINNE",
    			"status": "DØD"
    		}
    	]
    }
    """.trimIndent()

    private fun hentPersonResponse(
            medAktørId: String,
            medFødselsdato: LocalDate,
            medFornavn: String,
            medMellomnavn: String? = null,
            medEtternavn: String,
            medStatus : String
    ) = HentPersonResponse().apply {
        person = Person().apply {
            aktoer = AktoerId().apply {
                aktoerId = medAktørId
            }
            personnavn = Personnavn().apply {
                etternavn = medEtternavn
                mellomnavn = medMellomnavn
                fornavn = medFornavn
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
                foedselsdato = medFødselsdato.toXmlGregorianCalendar()
            }
            statsborgerskap = Statsborgerskap().apply {
                land = Landkoder().apply {
                    value = "NOR"
                }
            }
            personstatus = Personstatus().apply {
                personstatus = Personstatuser().apply {
                    value = medStatus
                }
            }
        }
    }

    private fun hentFamilierelasjonerResponse() = HentPersonResponse().apply {
        person = Person().apply {
            aktoer = AktoerId().apply {
                aktoerId = "100000000000"
            }
            personnavn = Personnavn().apply {
                etternavn = "OPPSLAG"
                mellomnavn = "OPPSLAG"
                fornavn = "OPPSLAGSEN"
            }
            kjoenn = Kjoenn().apply {
                kjoenn = Kjoennstyper().apply {
                    value = "K"
                }
            }
            withHarFraRolleI(listOf(
                    Familierelasjon().apply {
                        tilRolle = Familierelasjoner().apply {
                            value = "BARN"
                        }
                        tilPerson = Person().apply {
                            aktoer = AktoerId().apply {
                                aktoerId = "100000000001"
                            }
                            personnavn = Personnavn().apply {
                                etternavn = "BARN"
                                mellomnavn = "EN"
                                fornavn = "BARNESEN"
                            }
                        }
                    },
                    Familierelasjon().apply {
                        tilRolle = Familierelasjoner().apply {
                            value = "BARN"
                        }
                        tilPerson = Person().apply {
                            aktoer = AktoerId().apply {
                                aktoerId = "100000000002"
                            }
                            personnavn = Personnavn().apply {
                                etternavn = "BARN"
                                mellomnavn = "TO"
                                fornavn = "BARNESEN"
                            }
                        }
                    },
                    Familierelasjon().apply {
                        tilRolle = Familierelasjoner().apply {
                            value = "MORA"
                        }
                        tilPerson = Person().apply {
                            aktoer = AktoerId().apply {
                                aktoerId = "100000000005"
                            }
                            personnavn = Personnavn().apply {
                                etternavn = "MOR"
                                mellomnavn = "MOR"
                                fornavn = "MORESEN"
                            }
                        }
                    },
                    Familierelasjon().apply {
                        tilRolle = Familierelasjoner().apply {
                            value = "FARA"
                        }
                        tilPerson = Person().apply {
                            aktoer = AktoerId().apply {
                                aktoerId = "100000000006"
                            }
                            personnavn = Personnavn().apply {
                                etternavn = "FAR"
                                mellomnavn = "FAR"
                                fornavn = "FARSEN"
                            }
                        }
                    },
                    Familierelasjon().apply {
                        tilRolle = Familierelasjoner().apply {
                            value = "BARN"
                        }
                        tilPerson = Person().apply {
                            aktoer = AktoerId().apply {
                                aktoerId = "100000000003"
                            }
                            personnavn = Personnavn().apply {
                                etternavn = "BARN"
                                mellomnavn = "TRE"
                                fornavn = "BARNSEN"
                            }
                        }
                    },
                    Familierelasjon().apply {
                        tilRolle = Familierelasjoner().apply {
                            value = "BARN"
                        }
                        tilPerson = Person().apply {
                            aktoer = AktoerId().apply {
                                aktoerId = "100000000004"
                            }
                            personnavn = Personnavn().apply {
                                etternavn = "BARN"
                                mellomnavn = "FIRE"
                                fornavn = "BARNSEN"
                            }
                        }
                    },
                    Familierelasjon().apply {
                        tilRolle = Familierelasjoner().apply {
                            value = "BARN"
                        }
                        endringstype = Endringstyper.SLETTET
                        tilPerson = Person().apply {
                            aktoer = AktoerId().apply {
                                aktoerId = "100000000005"
                            }
                            personnavn = Personnavn().apply {
                                etternavn = "BARN"
                                mellomnavn = "FEM"
                                fornavn = "BARNSEN"
                            }
                        }
                    }
            ))
            foedselsdato = Foedselsdato().apply {
                foedselsdato = LocalDate.now().minusYears(1).toXmlGregorianCalendar()
            }
            statsborgerskap = Statsborgerskap().apply {
                land = Landkoder().apply {
                    value = "NOR"
                }
            }
            personstatus = Personstatus().apply {
                personstatus = Personstatuser().apply {
                    value = "BOSA"
                }
            }
        }
    }
}