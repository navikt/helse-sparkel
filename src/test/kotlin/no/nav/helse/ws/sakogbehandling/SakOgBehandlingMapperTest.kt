package no.nav.helse.ws.sakogbehandling

import no.nav.helse.common.*
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.finnsakogbehandlingskjedeliste.*
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.finnsakogbehandlingskjedeliste.Sak
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.sakogbehandling.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.*

class SakOgBehandlingMapperTest {

    @Test
    fun sisteBehandlingAvgjørStatus() {
        val sOgBSak = barebonesSak().apply { behandlingskjede.addAll(behandlingsliste()) }
        val expected = Sak("" +
                "el sako grande",
                "temaet",
                LocalDate.parse("2019-02-22"),
                LocalDate.parse("2019-02-26"),
                "opp og avgjort")
        val actual = mapSak(sOgBSak)
        assertEquals(expected, actual)
    }

    @Test
    fun behandlingSomManglerInfo() {
        val sOgBSak = barebonesSak().apply { behandlingskjede.add(Behandlingskjede()) }
        val expected = Sak("" +
                "el sako grande",
                "temaet",
                LocalDate.parse("2019-02-22"))
        val actual = mapSak(sOgBSak)
        assertEquals(expected, actual)
    }

    @Test
    fun sakUtenBehandlinger() {
        val sOgBSak = Sak().apply {
            opprettet = LocalDate.parse("2019-02-22").toXmlGregorianCalendar()
            saksId = "el sako grande"
            sakstema = Sakstemaer().apply {
                value = "temaet"
            }
        }
        val expected = Sak("" +
                "el sako grande",
                "temaet",
                LocalDate.parse("2019-02-22"))
        val actual = mapSak(sOgBSak)
        assertEquals(expected, actual)
    }

    private fun barebonesSak() = Sak().apply {
        opprettet = LocalDate.parse("2019-02-22").toXmlGregorianCalendar()
        saksId = "el sako grande"
        sakstema = Sakstemaer().apply {
            value = "temaet"
        }
    }

    private fun behandlingsliste() = listOf(
            Behandlingskjede().apply {
                slutt = LocalDate.parse("2019-02-25").toXmlGregorianCalendar()
                sisteBehandlingsstatus = Behandlingsstatuser().apply {
                    value = "ikke helt klar enda"
                }
            },
            Behandlingskjede().apply {
                slutt = LocalDate.parse("2019-02-26").toXmlGregorianCalendar()
                sisteBehandlingsstatus = Behandlingsstatuser().apply {
                    value = "opp og avgjort"
                }
            }
    )

}
