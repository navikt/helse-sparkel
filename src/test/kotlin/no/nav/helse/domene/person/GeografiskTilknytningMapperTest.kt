package no.nav.helse.domene.person

import no.nav.helse.domene.person.domain.Diskresjonskode
import no.nav.helse.domene.person.domain.GeografiskOmraade
import no.nav.helse.domene.person.domain.GeografiskTilknytning
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bydel
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GeografiskTilknytningMapperTest {
    @Test
    fun `skal mappe geografisk enhet med tilknytning`() {
        val response = no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse().apply {
            geografiskTilknytning = Bydel().apply {
                geografiskTilknytning = "030103"
            }
        }
        val expected = GeografiskTilknytning(null, GeografiskOmraade("BYDEL", "030103"))
        val actual = GeografiskTilknytningMapper.tilGeografiskTilknytning(response)
        assertEquals(expected, actual)
    }

    @Test
    fun `skal mappe diskresjonskode med ukjent tilknytning`() {
        val response = no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse().apply {
            diskresjonskode = Diskresjonskoder().apply {
                value = "SPSF"
            }
        }
        val expected = GeografiskTilknytning(
                diskresjonskode = Diskresjonskode("SPSF", "Sperret adresse, strengt fortrolig", kode = 6),
                geografiskOmraade = null
        )
        val actual = GeografiskTilknytningMapper.tilGeografiskTilknytning(response)
        assertEquals(expected, actual)
    }

    @Test
    fun `skal håndtere at diskresjonskode og geografisk område er null`() {
        val response = no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse()
        val expected = GeografiskTilknytning(null, null)
        val actual = GeografiskTilknytningMapper.tilGeografiskTilknytning(response)
        assertEquals(expected, actual)
    }

    @Test
    fun `skal mappe geografisk enhet med diskresjonskode 6 og geografisk område`() {
        val response = no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse().apply {
            diskresjonskode = Diskresjonskoder().apply {
                value = "SPSF"
            }
            geografiskTilknytning = Bydel().apply {
                geografiskTilknytning = "030103"
            }
        }
        val expected = GeografiskTilknytning(
                diskresjonskode = Diskresjonskode("SPSF", "Sperret adresse, strengt fortrolig", kode = 6),
                geografiskOmraade = GeografiskOmraade("BYDEL", "030103")
        )
        val actual = GeografiskTilknytningMapper.tilGeografiskTilknytning(response)

        assertEquals(expected, actual)
    }

    @Test
    fun `skal mappe geografisk enhet med diskresjonskode 7 og geografisk område`() {
        val response = no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse().apply {
            diskresjonskode = Diskresjonskoder().apply {
                value = "SPFO"
            }
            geografiskTilknytning = Bydel().apply {
                geografiskTilknytning = "030103"
            }
        }
        val expected = GeografiskTilknytning(
                diskresjonskode = Diskresjonskode("SPFO", "Sperret adresse, fortrolig", kode = 7),
                geografiskOmraade = GeografiskOmraade("BYDEL", "030103")
        )
        val actual = GeografiskTilknytningMapper.tilGeografiskTilknytning(response)

        assertEquals(expected, actual)
    }

    @Test
    fun `skal mappe geografisk enhet med ukjent diskresjonskode`() {
        val response = no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse().apply {
            diskresjonskode = Diskresjonskoder().apply {
                value = "UKJENT KODE"
            }
            geografiskTilknytning = Bydel().apply {
                geografiskTilknytning = "030103"
            }
        }
        val expected = GeografiskTilknytning(
                diskresjonskode = Diskresjonskode("UKJENT KODE"),
                geografiskOmraade = GeografiskOmraade("BYDEL", "030103")
        )
        val actual = GeografiskTilknytningMapper.tilGeografiskTilknytning(response)

        assertEquals(expected, actual)
    }

}
