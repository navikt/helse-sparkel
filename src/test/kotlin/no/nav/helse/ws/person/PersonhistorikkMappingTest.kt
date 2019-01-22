package no.nav.helse.ws.person

import no.nav.helse.ws.*
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.*

class PersonhistorikkMappingTest {

    @Test
    fun withStatsborgerskap() {
        val expected = Personhistorikk(
                AktørId("1234567891011"),
                listOf(
                        TidsperiodeMedVerdi("NO", LocalDate.of(2019, 1, 21),
                                LocalDate.of(2019, 1, 22))
                ),
                emptyList(),
                emptyList()
        )
        val actual = PersonhistorikkMapper.toPersonhistorikk(responseWithStatsborgerskap())
        assertEquals(expected, actual)
    }

    @Test
    fun withPersonstatus() {
        val expected = Personhistorikk(
                AktørId("1234567891011"),
                emptyList(),
                listOf(
                        TidsperiodeMedVerdi("statusen", LocalDate.of(2019, 1, 21),
                                LocalDate.of(2019, 1, 22))
                ),
                emptyList()
        )
        val actual = PersonhistorikkMapper.toPersonhistorikk(responseWithStatus())
        assertEquals(expected, actual)
    }

    @Test
    fun withGateadresse() {
        val expected = Personhistorikk(
                AktørId("1234567891011"),
                emptyList(),
                emptyList(),
                listOf(
                        TidsperiodeMedVerdi("veien 2A, 1234", LocalDate.of(2019, 1, 21),
                                LocalDate.of(2019, 1, 22))
                )
        )
        val actual = PersonhistorikkMapper.toPersonhistorikk(responseWithGateadresse())
        assertEquals(expected, actual)
    }

    @Test
    fun withPostboksadresse() {
        val expected = Personhistorikk(
                AktørId("1234567891011"),
                emptyList(),
                emptyList(),
                listOf(
                        TidsperiodeMedVerdi("Boksanlegget, 1234", LocalDate.of(2019, 1, 21),
                                LocalDate.of(2019, 1, 22))
                )
        )
        val actual = PersonhistorikkMapper.toPersonhistorikk(responseWithPostboksadresse())
        assertEquals(expected, actual)
    }

    private fun responseWithGateadresse(): HentPersonhistorikkResponse {
        val periode = BostedsadressePeriode().apply {
            periode = periode()
            withBostedsadresse(gateadresse())
        }
        return HentPersonhistorikkResponse().apply {
            aktoer = aktør()
            withBostedsadressePeriodeListe(periode)
        }
    }

    private fun responseWithPostboksadresse(): HentPersonhistorikkResponse {
        val periode = BostedsadressePeriode().apply {
            periode = periode()
            withBostedsadresse(postboksadresse())
        }
        return HentPersonhistorikkResponse().apply {
            aktoer = aktør()
            withBostedsadressePeriodeListe(periode)
        }
    }

    private fun responseWithStatsborgerskap(): HentPersonhistorikkResponse {
        val periode = StatsborgerskapPeriode().apply {
            periode = periode()
            withStatsborgerskap(statsborgerskap())
        }
        return HentPersonhistorikkResponse().apply {
            aktoer = aktør()
            withStatsborgerskapListe(periode)
        }
    }

    private fun responseWithStatus(): HentPersonhistorikkResponse {
        val periode = PersonstatusPeriode().apply {
            periode = periode()
            withPersonstatus(Personstatuser().withValue("statusen"))
        }
        return HentPersonhistorikkResponse().apply {
            aktoer = aktør()
            withPersonstatusListe(periode)
        }
    }

    private fun aktør() = AktoerId().apply { aktoerId = "1234567891011" }

    private fun gateadresse(): Bostedsadresse = Bostedsadresse().apply {
        strukturertAdresse = Gateadresse().apply {
            gatenavn = "veien"
            husnummer = 2
            husbokstav = "A"
            withPoststed(Postnummer().apply { value = "1234" })
        }
    }

    private fun postboksadresse(): Bostedsadresse = Bostedsadresse().apply {
        strukturertAdresse = PostboksadresseNorsk().apply {
            withPostboksanlegg("Boksanlegget")
            withPoststed(Postnummer().withValue("1234"))
        }
    }

    private fun statsborgerskap() = Statsborgerskap().apply {
        land = Landkoder().withValue("NO")
    }

    private fun periode() = Periode()
            .withFom(LocalDate.of(2019, 1, 21).toXmlGregorianCalendar())
            .withTom(LocalDate.of(2019, 1, 22).toXmlGregorianCalendar())

}

