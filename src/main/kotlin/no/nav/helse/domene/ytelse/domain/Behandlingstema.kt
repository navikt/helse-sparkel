package no.nav.helse.domene.ytelse.domain

sealed class Behandlingstema(val kode: String, val tema: Tema) {

    object Foreldrepenger: Behandlingstema("FP", Tema.Foreldrepenger)
    object ForeldrepengerMedFødsel: Behandlingstema("FØ", Tema.Foreldrepenger)
    object ForeldrepengerMedFødselUtland: Behandlingstema("FU", Tema.Foreldrepenger)
    object ForeldrepengerMedAdopsjon: Behandlingstema("AP", Tema.Foreldrepenger)
    object Svangerskapspenger: Behandlingstema("SV", Tema.Foreldrepenger)
    object EngangstønadMedAdopsjon: Behandlingstema("AE", Tema.Foreldrepenger)
    object EngangstønadMedFødsel: Behandlingstema("FE", Tema.Foreldrepenger)

    object Omsorgspenger: Behandlingstema("OM", Tema.PårørendeSykdom)
    object Opplæringspenger: Behandlingstema("OP", Tema.PårørendeSykdom)
    object PleiepengerSyktBarnFør2017_10_01: Behandlingstema("PB", Tema.PårørendeSykdom)
    object PleiepengerFør2017_10_01: Behandlingstema("PI", Tema.PårørendeSykdom)
    object PleiepengerPårørende: Behandlingstema("PP", Tema.PårørendeSykdom)
    object Pleiepenger: Behandlingstema("PN", Tema.PårørendeSykdom)

    object Sykepenger: Behandlingstema("SP", Tema.Sykepenger)
    object SykepengerForsikringRisikoSykefravær: Behandlingstema("RS", Tema.Sykepenger)
    object SykepengerReisetilskudd: Behandlingstema("RT", Tema.Sykepenger)
    object SykepengerUtenlandsopphold: Behandlingstema("SU", Tema.Sykepenger)

    object Overgangsstønad: Behandlingstema("OG", Tema.EnsligForsørger)

    class Ukjent(kode: String): Behandlingstema(kode, Tema.Ukjent("??"))

    fun name() = when (this) {
        is Foreldrepenger -> "Foreldrepenger"
        is ForeldrepengerMedFødsel -> "ForeldrepengerMedFødsel"
        is ForeldrepengerMedFødselUtland -> "ForeldrepengerMedFødselUtland"
        is ForeldrepengerMedAdopsjon -> "ForeldrepengerMedAdopsjon"
        is Svangerskapspenger -> "Svangerskapspenger"
        is EngangstønadMedAdopsjon -> "EngangstønadMedAdopsjon"
        is EngangstønadMedFødsel -> "EngangstønadMedFødsel"
        is Omsorgspenger -> "Omsorgspenger"
        is Opplæringspenger -> "Opplæringspenger"
        is PleiepengerSyktBarnFør2017_10_01 -> "PleiepengerSyktBarnFør2017_10_01"
        is PleiepengerFør2017_10_01 -> "PleiepengerFør2017_10_01"
        is PleiepengerPårørende -> "PleiepengerPårørende"
        is Pleiepenger -> "Pleiepenger"
        is Sykepenger -> "Sykepenger"
        is SykepengerForsikringRisikoSykefravær -> "SykepengerForsikringRisikoSykefravær"
        is SykepengerReisetilskudd -> "SykepengerReisetilskudd"
        is SykepengerUtenlandsopphold -> "SykepengerUtenlandsopphold"
        is Overgangsstønad -> "Overgangsstønad"
        is Ukjent -> "Ukjent"
    }

    override fun toString(): String {
        return "${name()}(kode='$kode', tema=$tema)"
    }

    companion object {
        fun fraKode(kode: String) = when (kode) {
            Foreldrepenger.kode -> Foreldrepenger
            ForeldrepengerMedFødsel.kode -> ForeldrepengerMedFødsel
            ForeldrepengerMedFødselUtland.kode -> ForeldrepengerMedFødselUtland
            ForeldrepengerMedAdopsjon.kode -> ForeldrepengerMedAdopsjon
            Svangerskapspenger.kode -> Svangerskapspenger
            EngangstønadMedAdopsjon.kode -> EngangstønadMedAdopsjon
            EngangstønadMedFødsel.kode -> EngangstønadMedFødsel
            Omsorgspenger.kode -> Omsorgspenger
            Opplæringspenger.kode -> Opplæringspenger
            PleiepengerSyktBarnFør2017_10_01.kode -> PleiepengerSyktBarnFør2017_10_01
            PleiepengerFør2017_10_01.kode -> PleiepengerFør2017_10_01
            PleiepengerPårørende.kode -> PleiepengerPårørende
            Pleiepenger.kode -> Pleiepenger
            Sykepenger.kode -> Sykepenger
            SykepengerForsikringRisikoSykefravær.kode -> SykepengerForsikringRisikoSykefravær
            SykepengerReisetilskudd.kode -> SykepengerReisetilskudd
            SykepengerUtenlandsopphold.kode -> SykepengerUtenlandsopphold
            Overgangsstønad.kode -> Overgangsstønad
            else -> Ukjent(kode)
        }
    }
}
