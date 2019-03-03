package no.nav.helse.ws.arbeidsfordeling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Either
import no.nav.helse.Feilårsak
import no.nav.helse.ws.AktørId
import no.nav.helse.ws.person.Diskresjonskode
import no.nav.helse.ws.person.GeografiskOmraade
import no.nav.helse.ws.person.GeografiskTilknytning
import no.nav.helse.ws.person.PersonService
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.FinnBehandlendeEnhetListeUgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.feil.UgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Enhetsstatus
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Organisasjonsenhet
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ArbeidsfordelingServiceTest {
    @Test
    fun `skal returnere feil når personoppslag for hovedaktør gir feil`() {
        val aktørId = AktørId("1831212532200")
        val tema = Tema("SYK")
        val expected = Either.Left(Feilårsak.UkjentFeil)

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
            is Either.Left -> {
                Assertions.assertEquals(expected, actual)
            }
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal returnere feil når personoppslag for medaktør gir feil`() {
        val aktørId = AktørId("1831212532200")
        val medaktørId = AktørId("1831212532201")
        val tema = Tema("SYK")
        val expected = Either.Left(Feilårsak.UkjentFeil)

        val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
        val personService = mockk<PersonService>()

        every {
            personService.geografiskTilknytning(match {
                it == aktørId
            })
        } returns Either.Right(GeografiskTilknytning(null, null))

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
            is Either.Left -> {
                Assertions.assertEquals(expected, actual)
            }
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal mappe FinnBehandlendeEnhetListeUgyldigInput til FeilFraTjeneste`() {
        val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
        val personService = mockk<PersonService>()

        every {
            personService.geografiskTilknytning(any())
        } returns GeografiskTilknytning(null, GeografiskOmraade("Bydel", "030103")).let {
            Either.Right(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(any(), any())
        } returns Either.Left(FinnBehandlendeEnhetListeUgyldigInput("SOAP fault", UgyldigInput()))

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(AktørId("123456789"), emptyList(), Tema("SYK"))

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.FeilFraTjeneste, actual.left)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal mappe Exception til UkjentFeil`() {
        val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
        val personService = mockk<PersonService>()

        every {
            personService.geografiskTilknytning(any())
        } returns GeografiskTilknytning(null, GeografiskOmraade("Bydel", "030103")).let {
            Either.Right(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(any(), any())
        } returns Either.Left(Exception("SOAP fault"))

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(AktørId("123456789"), emptyList(), Tema("SYK"))

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.UkjentFeil, actual.left)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
        }
    }

    @Test
    fun `skal mappe ingen resultat til IkkeFunnet`() {
        val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
        val personService = mockk<PersonService>()

        every {
            personService.geografiskTilknytning(any())
        } returns GeografiskTilknytning(null, GeografiskOmraade("Bydel", "030103")).let {
            Either.Right(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(any(), any())
        } returns Either.Right(emptyList())

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(AktørId("123456789"), emptyList(), Tema("SYK"))

        when (actual) {
            is Either.Left -> assertEquals(Feilårsak.IkkeFunnet, actual.left)
            is Either.Right -> fail { "Expected Either.Left to be returned" }
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
            Either.Right(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(match {
                it == geografiskTilknytning
            }, match {
                it == tema
            })
        } returns Either.Right(listOf(Organisasjonsenhet().apply {
            enhetId = expected.id
            enhetNavn = expected.navn
            status = Enhetsstatus.AKTIV
        }))

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(aktørId, emptyList(), tema)

        when (actual) {
            is Either.Right -> {
                Assertions.assertEquals(expected, actual.right)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
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
            Either.Right(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(match {
                it == geografiskTilknytning
            }, match {
                it == tema
            })
        } returns Either.Right(listOf(Organisasjonsenhet().apply {
            enhetId = expected.id
            enhetNavn = expected.navn
            status = Enhetsstatus.AKTIV
        }))

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(aktørId, emptyList(), tema)

        when (actual) {
            is Either.Right -> {
                Assertions.assertEquals(expected, actual.right)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
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
            Either.Right(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(match {
                it == geografiskTilknytning
            }, match {
                it == tema
            })
        } returns Either.Right(listOf(Organisasjonsenhet().apply {
            enhetId = expected.id
            enhetNavn = expected.navn
            status = Enhetsstatus.AKTIV
        }))

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(aktørId, emptyList(), tema)

        when (actual) {
            is Either.Right -> {
                Assertions.assertEquals(expected, actual.right)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
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
            Either.Right(it)
        }

        every {
            personService.geografiskTilknytning(match {
                it == medaktørIdIkkeKode6
            })
        } returns geografiskTilknytningMedaktørIkkeKode6.let {
            Either.Right(it)
        }

        every {
            personService.geografiskTilknytning(match {
                it == medaktørIdKode6
            })
        } returns geografiskTilknytningMedaktørKode6.let {
            Either.Right(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(match {
                it == geografiskTilknytningMedaktørKode6
            }, match {
                it == tema
            })
        } returns Either.Right(listOf(Organisasjonsenhet().apply {
            enhetId = expected.id
            enhetNavn = expected.navn
            status = Enhetsstatus.AKTIV
        }))

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(hovedAktørId, listOf(
                medaktørIdIkkeKode6, medaktørIdKode6
        ), tema)

        when (actual) {
            is Either.Right -> {
                Assertions.assertEquals(expected, actual.right)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
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
            Either.Right(it)
        }

        every {
            personService.geografiskTilknytning(match {
                it == medaktørIdIkkeKode6
            })
        } returns geografiskTilknytningMedaktørIkkeKode6.let {
            Either.Right(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(match {
                it == geografiskTilknytningHovedaktør
            }, match {
                it == tema
            })
        } returns Either.Right(listOf(Organisasjonsenhet().apply {
            enhetId = expected.id
            enhetNavn = expected.navn
            status = Enhetsstatus.AKTIV
        }))

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(hovedAktørId, listOf(
                medaktørIdIkkeKode6
        ), tema)

        when (actual) {
            is Either.Right -> {
                Assertions.assertEquals(expected, actual.right)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
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
            Either.Right(it)
        }

        every {
            arbeidsfordelingClient.getBehandlendeEnhet(match {
                it == geografiskTilknytning
            }, match {
                it == tema
            })
        } returns Either.Right(listOf(Organisasjonsenhet().apply {
            enhetId = expected.id
            enhetNavn = expected.navn
            status = Enhetsstatus.AKTIV
        }))

        val arbeidsfordelingService = ArbeidsfordelingService(arbeidsfordelingClient, personService)

        val actual = arbeidsfordelingService.getBehandlendeEnhet(aktørId, emptyList(), tema)

        when (actual) {
            is Either.Right -> {
                Assertions.assertEquals(expected, actual.right)
            }
            is Either.Left -> fail { "Expected Either.Right to be returned" }
        }
    }
}
