package no.nav.helse.domene.ytelse.domain

import no.nav.helse.domene.ytelse.domain.Behandlingstema.Companion.fraKode
import no.nav.helse.domene.ytelse.domain.Tema.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BehandlingstemaTest {

    @ParameterizedTest
    @ValueSource(strings = ["SV"])
    fun `kodeverkverdier for behandlingstema for svangerskapspenger`(behandlingstema: String) {
        assertEquals(Foreldrepenger, fraKode(behandlingstema).tema)
    }

    @ParameterizedTest
    @ValueSource(strings = ["FØ", "AP", "FU", "FP"])
    fun `kodeverkverdier for behandlingstema for foreldrepenger`(behandlingstema: String) {
        assertEquals(Foreldrepenger, fraKode(behandlingstema).tema)
    }

    @ParameterizedTest
    @ValueSource(strings = ["AE", "FE"])
    fun `kodeverkverdier for behandlingstema for engangstønad`(behandlingstema: String) {
        assertEquals(Foreldrepenger, fraKode(behandlingstema).tema)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SP", "SU", "RT", "RS"])
    fun `kodeverkverdier for behandlingstema for sykepenger`(behandlingstema: String) {
        assertEquals(Sykepenger, fraKode(behandlingstema).tema)
    }

    @ParameterizedTest
    @ValueSource(strings = ["OM", "OP", "PB", "PI", "PP", "PN"])
    fun `kodeverkverdier for behandlingstema for pårørende sykdom`(behandlingstema: String) {
        assertEquals(PårørendeSykdom, fraKode(behandlingstema).tema)
    }

    @ParameterizedTest
    @ValueSource(strings = ["OG"])
    fun `kodeverkverdier for behandlingstema for enslig forsørger`(behandlingstema: String) {
        assertEquals(EnsligForsørger, fraKode(behandlingstema).tema)
    }

    @Test
    fun `skal mappe til behandlingstema`() {
        val expected = mapOf(
                "FP" to Behandlingstema.Foreldrepenger::class,
                "FØ" to Behandlingstema.ForeldrepengerMedFødsel::class,
                "FU" to Behandlingstema.ForeldrepengerMedFødselUtland::class,
                "AP" to Behandlingstema.ForeldrepengerMedAdopsjon::class,
                "SV" to Behandlingstema.Svangerskapspenger::class,
                "AE" to Behandlingstema.EngangstønadMedAdopsjon::class,
                "FE" to Behandlingstema.EngangstønadMedFødsel::class,
                "OM" to Behandlingstema.Omsorgspenger::class,
                "OP" to Behandlingstema.Opplæringspenger::class,
                "PB" to Behandlingstema.PleiepengerSyktBarnFør2017_10_01::class,
                "PI" to Behandlingstema.PleiepengerFør2017_10_01::class,
                "PP" to Behandlingstema.PleiepengerPårørende::class,
                "PN" to Behandlingstema.Pleiepenger::class,
                "SP" to Behandlingstema.Sykepenger::class,
                "RS" to Behandlingstema.SykepengerForsikringRisikoSykefravær::class,
                "RT" to Behandlingstema.SykepengerReisetilskudd::class,
                "SU" to Behandlingstema.SykepengerUtenlandsopphold::class,
                "OG" to Behandlingstema.Overgangsstønad::class,
                "ZZ" to Behandlingstema.Ukjent::class
        )

        expected.forEach { kodeverdi, kclass ->
            assertEquals(kclass, fraKode(kodeverdi)::class)
        }
    }

    @Test
    fun `skal ha riktig string-representasjon`() {
        val expected = mapOf(
                "FP" to "Foreldrepenger(kode='FP', tema=Foreldrepenger)",
                "FØ" to "ForeldrepengerMedFødsel(kode='FØ', tema=Foreldrepenger)",
                "FU" to "ForeldrepengerMedFødselUtland(kode='FU', tema=Foreldrepenger)",
                "AP" to "ForeldrepengerMedAdopsjon(kode='AP', tema=Foreldrepenger)",
                "SV" to "Svangerskapspenger(kode='SV', tema=Foreldrepenger)",
                "AE" to "EngangstønadMedAdopsjon(kode='AE', tema=Foreldrepenger)",
                "FE" to "EngangstønadMedFødsel(kode='FE', tema=Foreldrepenger)",
                "OM" to "Omsorgspenger(kode='OM', tema=PårørendeSykdom)",
                "OP" to "Opplæringspenger(kode='OP', tema=PårørendeSykdom)",
                "PB" to "PleiepengerSyktBarnFør2017_10_01(kode='PB', tema=PårørendeSykdom)",
                "PI" to "PleiepengerFør2017_10_01(kode='PI', tema=PårørendeSykdom)",
                "PP" to "PleiepengerPårørende(kode='PP', tema=PårørendeSykdom)",
                "PN" to "Pleiepenger(kode='PN', tema=PårørendeSykdom)",
                "SP" to "Sykepenger(kode='SP', tema=Sykepenger)",
                "RS" to "SykepengerForsikringRisikoSykefravær(kode='RS', tema=Sykepenger)",
                "RT" to "SykepengerReisetilskudd(kode='RT', tema=Sykepenger)",
                "SU" to "SykepengerUtenlandsopphold(kode='SU', tema=Sykepenger)",
                "OG" to "Overgangsstønad(kode='OG', tema=EnsligForsørger)",
                "ZZ" to "Ukjent(kode='ZZ', tema=Ukjent(tema=??))"
        )

        expected.forEach { kodeverdi, string ->
            assertEquals(string, fraKode(kodeverdi).toString())
        }
    }
}
