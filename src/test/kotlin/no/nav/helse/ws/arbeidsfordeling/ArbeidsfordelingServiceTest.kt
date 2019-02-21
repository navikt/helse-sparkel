package no.nav.helse.ws.arbeidsfordeling

import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Feil
import no.nav.helse.OppslagResult
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.person.Diskresjonskode
import no.nav.helse.ws.person.GeografiskOmraade
import no.nav.helse.ws.person.GeografiskTilknytning
import no.nav.helse.ws.person.PersonService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ArbeidsfordelingServiceTest {
    @Test
    fun `skal returnere feil når personoppslag for hovedaktør gir feil`() {
        val aktørId = AktørId("1831212532200")
        val tema = Tema("SYK")
        val expected = OppslagResult.Feil(HttpStatusCode.InternalServerError, Feil.Feilmelding("En feil oppstod"))

        val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
        val personService = mockk<PersonService>()

        every {
            personService.geografiskTilknytning(match {
                it == aktørId
            })
        } returns expected

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(aktørId, emptyList(), tema)

        verify(exactly = 1) {
            personService.geografiskTilknytning(any())
        }

        when (actual) {
            is OppslagResult.Feil -> {
                Assertions.assertEquals(expected, actual)
            }
            is OppslagResult.Ok -> fail { "Expected OppslagResult.Feil to be returned" }
        }
    }

    @Test
    fun `skal returnere feil når personoppslag for medaktør gir feil`() {
        val aktørId = AktørId("1831212532200")
        val medaktørId = AktørId("1831212532201")
        val tema = Tema("SYK")
        val expected = OppslagResult.Feil(HttpStatusCode.InternalServerError, Feil.Feilmelding("En feil oppstod"))

        val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
        val personService = mockk<PersonService>()

        every {
            personService.geografiskTilknytning(match {
                it == aktørId
            })
        } returns OppslagResult.Ok(GeografiskTilknytning(null, null))

        every {
            personService.geografiskTilknytning(match {
                it == medaktørId
            })
        } returns expected

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(aktørId, listOf(medaktørId), tema)

        verify(exactly = 2) {
            personService.geografiskTilknytning(any())
        }

        when (actual) {
            is OppslagResult.Feil -> {
                Assertions.assertEquals(expected, actual)
            }
            is OppslagResult.Ok -> fail { "Expected OppslagResult.Feil to be returned" }
        }
    }

    @Test
    fun `Finn enhet for hovedaktør uten kode 6 uten noen medaktører`() {
        val aktørId = AktørId("1831212532200")
        val tema = Tema("SYK")
        val expected = Enhet("4432", "NAV Arbeid og ytelser Follo")

        val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
        val personService = mockk<PersonService>()

        val geografiskTilknytning = GeografiskTilknytning(null, GeografiskOmraade("Bydel", "030103"))

        every {
            personService.geografiskTilknytning(match {
                it == aktørId
            })
        } returns geografiskTilknytning.let {
            OppslagResult.Ok(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(match {
                it == geografiskTilknytning
            }, match {
                it == tema
            })
        } returns expected.let {
            OppslagResult.Ok(it)
        }

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(aktørId, emptyList(), tema)

        when (actual) {
            is OppslagResult.Ok -> {
                Assertions.assertEquals(expected, actual.data)
            }
            is OppslagResult.Feil -> fail { "Expected OppslagResult.Ok to be returned" }
        }
    }

    @Test
    fun `Finn enhet for hovedaktør med kode 6 uten noen medaktører`() {
        val aktørId = AktørId("1831212532200")
        val tema = Tema("SYK")
        val expected = Enhet("2103", "NAV Viken")

        val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
        val personService = mockk<PersonService>()

        val geografiskTilknytning = GeografiskTilknytning(
                Diskresjonskode("SPSF", "Sperret adresse, strengt fortrolig", kode = 6),
                GeografiskOmraade("Bydel", "030103"))

        every {
            personService.geografiskTilknytning(match {
                it == aktørId
            })
        } returns geografiskTilknytning.let {
            OppslagResult.Ok(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(match {
                it == geografiskTilknytning
            }, match {
                it == tema
            })
        } returns expected.let {
            OppslagResult.Ok(it)
        }

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(aktørId, emptyList(), tema)

        when (actual) {
            is OppslagResult.Ok -> {
                Assertions.assertEquals(expected, actual.data)
            }
            is OppslagResult.Feil -> fail { "Expected OppslagResult.Ok to be returned" }
        }
    }

    @Test
    fun `Finn enhet for hovedaktør med kode 7 uten noen medaktører`() {
        val aktørId = AktørId("1831212532200")
        val tema = Tema("SYK")
        val expected = Enhet("2103", "NAV Viken")

        val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
        val personService = mockk<PersonService>()

        val geografiskTilknytning = GeografiskTilknytning(
                Diskresjonskode("SPSF", "Sperret adresse, strengt fortrolig", kode = 7),
                GeografiskOmraade("Bydel", "030103"))

        every {
            personService.geografiskTilknytning(match {
                it == aktørId
            })
        } returns geografiskTilknytning.let {
            OppslagResult.Ok(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(match {
                it == geografiskTilknytning
            }, match {
                it == tema
            })
        } returns expected.let {
            OppslagResult.Ok(it)
        }

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(aktørId, emptyList(), tema)

        when (actual) {
            is OppslagResult.Ok -> {
                Assertions.assertEquals(expected, actual.data)
            }
            is OppslagResult.Feil -> fail { "Expected OppslagResult.Ok to be returned" }
        }
    }

    @Test
    fun `Finn enhet for hovedaktør uten kode 6 og medaktør med kode 6`() {
        val hovedAktørId = AktørId("1831212532200")
        val medaktørIdIkkeKode6 = AktørId("1831212532201")
        val medaktørIdKode6 = AktørId("1831212532202")

        val tema = Tema("SYK")
        val expected = Enhet("2103", "NAV Viken")

        val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
        val personService = mockk<PersonService>()

        val geografiskTilknytningHovedaktør = GeografiskTilknytning(null, GeografiskOmraade("Bydel", "030103"))
        val geografiskTilknytningMedaktørIkkeKode6 = GeografiskTilknytning(null, GeografiskOmraade("Bydel", "030104"))
        val geografiskTilknytningMedaktørKode6 = GeografiskTilknytning(Diskresjonskode("SPSF", kode = 6), GeografiskOmraade("Bydel", "030105"))

        every {
            personService.geografiskTilknytning(match {
                it == hovedAktørId
            })
        } returns geografiskTilknytningHovedaktør.let {
            OppslagResult.Ok(it)
        }

        every {
            personService.geografiskTilknytning(match {
                it == medaktørIdIkkeKode6
            })
        } returns geografiskTilknytningMedaktørIkkeKode6.let {
            OppslagResult.Ok(it)
        }

        every {
            personService.geografiskTilknytning(match {
                it == medaktørIdKode6
            })
        } returns geografiskTilknytningMedaktørKode6.let {
            OppslagResult.Ok(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(match {
                it == geografiskTilknytningMedaktørKode6
            }, match {
                it == tema
            })
        } returns expected.let {
            OppslagResult.Ok(it)
        }

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(hovedAktørId, listOf(
                medaktørIdIkkeKode6, medaktørIdKode6
        ), tema)

        when (actual) {
            is OppslagResult.Ok -> {
                Assertions.assertEquals(expected, actual.data)
            }
            is OppslagResult.Feil -> fail { "Expected OppslagResult.Ok to be returned" }
        }
    }

    @Test
    fun `Finn enhet for hovedaktør uten kode 6 og medaktør uten kode 6`() {
        val hovedAktørId = AktørId("1831212532200")
        val medaktørIdIkkeKode6 = AktørId("1831212532201")

        val tema = Tema("SYK")
        val expected = Enhet("4432", "NAV Arbeid og ytelser Follo")

        val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
        val personService = mockk<PersonService>()

        val geografiskTilknytningHovedaktør = GeografiskTilknytning(null, GeografiskOmraade("Bydel", "030103"))
        val geografiskTilknytningMedaktørIkkeKode6 = GeografiskTilknytning(null, GeografiskOmraade("Bydel", "030104"))

        every {
            personService.geografiskTilknytning(match {
                it == hovedAktørId
            })
        } returns geografiskTilknytningHovedaktør.let {
            OppslagResult.Ok(it)
        }

        every {
            personService.geografiskTilknytning(match {
                it == medaktørIdIkkeKode6
            })
        } returns geografiskTilknytningMedaktørIkkeKode6.let {
            OppslagResult.Ok(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(match {
                it == geografiskTilknytningHovedaktør
            }, match {
                it == tema
            })
        } returns expected.let {
            OppslagResult.Ok(it)
        }

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(hovedAktørId, listOf(
                medaktørIdIkkeKode6
        ), tema)

        when (actual) {
            is OppslagResult.Ok -> {
                Assertions.assertEquals(expected, actual.data)
            }
            is OppslagResult.Feil -> fail { "Expected OppslagResult.Ok to be returned" }
        }
    }

    @Test
    fun `Finn enhet for hovedaktør uten kode 6 og uten geografisk tilknytning`() {
        val aktørId = AktørId("1831212532200")
        val tema = Tema("SYK")
        val expected = Enhet("4432", "NAV Arbeid og ytelser Follo")

        val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
        val personService = mockk<PersonService>()

        val geografiskTilknytning = GeografiskTilknytning(null, null)

        every {
            personService.geografiskTilknytning(match {
                it == aktørId
            })
        } returns geografiskTilknytning.let {
            OppslagResult.Ok(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(match {
                it == geografiskTilknytning
            }, match {
                it == tema
            })
        } returns expected.let {
            OppslagResult.Ok(it)
        }

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(aktørId, emptyList(), tema)

        when (actual) {
            is OppslagResult.Ok -> {
                Assertions.assertEquals(expected, actual.data)
            }
            is OppslagResult.Feil -> fail { "Expected OppslagResult.Ok to be returned" }
        }
    }
}
